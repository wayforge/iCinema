package com.icinema.pages.player.core.hls

import android.util.Log
import java.util.ArrayDeque
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.roundToLong

@Singleton
class HlsPrefetchCoordinator @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cache: HlsPersistentCache,
    private val manifestRewriter: HlsManifestRewriter,
    private val adRuleStore: HlsAdRuleStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = Collections.synchronizedSet(mutableSetOf<String>())
    private val manifestPolicyLock = Any()
    private val manifestResourcePrefetch = mutableMapOf<String, Boolean>()
    private val queueLock = Any()
    private val pendingResources = ArrayDeque<QueuedResource>()
    private val queuedResources = mutableSetOf<String>()
    private var resourceWorkerRunning = false
    private val snapshotLock = Any()
    private val playlistSnapshots = LinkedHashMap<String, HlsPlaylistSnapshot>()
    private val adUrlsByPlaylist = mutableMapOf<String, MutableSet<String>>()

    private val episodeLock = Any()
    private var activeEpisodeRootUrl: String? = null
    private val episodeGeneration = AtomicLong(0L)

    fun prefetchManifest(manifestUrl: String, includeInitialResources: Boolean) {
        val shouldStart = synchronized(manifestPolicyLock) {
            manifestResourcePrefetch[manifestUrl] =
                (manifestResourcePrefetch[manifestUrl] == true) || includeInitialResources
            inFlight.add(manifestUrl)
        }
        if (!shouldStart) return
        scope.launch {
            try {
                runCatching {
                    val body = downloadPlaylistText(manifestUrl) ?: return@runCatching
                    runCatching { cache.writeText(manifestUrl, HLS_CONTENT_TYPE, body) }
                    recordManifestSnapshot(manifestUrl, body)
                    val includeResources = synchronized(manifestPolicyLock) {
                        manifestResourcePrefetch[manifestUrl] == true
                    }
                    if (includeResources) {
                        val resourceUrls = collectAllResourceUrls(manifestUrl, body)
                        enqueueResources(resourceUrls.take(INITIAL_RESOURCE_PREFETCH_LIMIT), generation = 0L)
                    }
                }.onFailure { error ->
                    Log.d(TAG, "prefetch manifest failed url=$manifestUrl error=${error.message}")
                }
            } finally {
                synchronized(manifestPolicyLock) {
                    manifestResourcePrefetch.remove(manifestUrl)
                    inFlight.remove(manifestUrl)
                }
            }
        }
    }

    /**
     * Full sequential episode cache (including ad TS — required by some CDNs).
     */
    fun startEpisodePrecache(rootUrl: String) {
        val generation: Long
        synchronized(episodeLock) {
            if (activeEpisodeRootUrl == rootUrl) {
                return
            }
            activeEpisodeRootUrl = rootUrl
            generation = episodeGeneration.incrementAndGet()
        }
        clearPendingQueue()
        clearAdMarks()
        scope.launch {
            delay(FULL_EPISODE_PRECACHE_DELAY_MS)
            if (!isGenerationActive(generation)) return@launch
            runCatching {
                precachePlaylistTree(rootUrl, generation, depth = 0)
            }.onFailure { error ->
                Log.d(TAG, "episode precache failed root=$rootUrl error=${error.message}")
            }
        }
    }

    fun continueEpisodePrecache(playlistUrl: String, playlist: String) {
        val generation = synchronized(episodeLock) {
            if (activeEpisodeRootUrl == null) return
            episodeGeneration.get()
        }
        recordManifestSnapshot(playlistUrl, playlist)
        rebuildAdMarksFromRules(playlistUrl)
        if (manifestRewriter.extractMediaSegments(playlistUrl, playlist).isEmpty()) {
            val children = manifestRewriter.extractChildManifestUrls(playlistUrl, playlist)
            if (children.isNotEmpty()) {
                scope.launch {
                    precachePlaylistTree(selectMediaVariant(children), generation, depth = 1)
                }
            }
            return
        }
        val resourceUrls = collectAllResourceUrls(playlistUrl, playlist).let { urls ->
            if (playlist.contains(ENDLIST_TAG, ignoreCase = true)) urls
            else urls.take(LIVE_WINDOW_RESOURCE_LIMIT)
        }
        enqueueResources(resourceUrls, generation)
    }

    fun cancelEpisodePrecache() {
        synchronized(episodeLock) {
            activeEpisodeRootUrl = null
            episodeGeneration.incrementAndGet()
        }
        clearPendingQueue()
        clearAdMarks()
    }

    fun prefetchResources(resourceUrls: List<String>) {
        enqueueResources(
            urls = resourceUrls.distinct().take(RESOURCE_PREFETCH_LIMIT),
            generation = episodeGeneration.get()
        )
    }

    fun recordManifestSnapshot(manifestUrl: String, playlist: String) {
        val segments = manifestRewriter.extractMediaSegments(manifestUrl, playlist)
        val snapshot = HlsPlaylistSnapshot(
            playlistUrl = manifestUrl,
            segments = segments,
            childManifestUrls = manifestRewriter.extractChildManifestUrls(manifestUrl, playlist),
            updatedAtMs = System.currentTimeMillis()
        )
        synchronized(snapshotLock) {
            playlistSnapshots.remove(manifestUrl)
            playlistSnapshots[manifestUrl] = snapshot
            while (playlistSnapshots.size > SNAPSHOT_LIMIT) {
                val eldestKey = playlistSnapshots.entries.firstOrNull()?.key ?: break
                playlistSnapshots.remove(eldestKey)
            }
        }
        rebuildAdMarksFromRules(manifestUrl)
    }

    fun resolveSegment(playlistUrl: String, playbackPositionMs: Long): HlsResolvedMediaSegment? {
        val snapshots = synchronized(snapshotLock) { playlistSnapshots.values.toList() }
        val direct = snapshots.firstOrNull { it.playlistUrl == playlistUrl }
        direct?.findSegment(playbackPositionMs)?.let { segment ->
            return HlsResolvedMediaSegment(direct.playlistUrl, segment)
        }
        direct?.childManifestUrls.orEmpty().forEach { childUrl ->
            val child = snapshots.firstOrNull { it.playlistUrl == childUrl } ?: return@forEach
            child.findSegment(playbackPositionMs)?.let { segment ->
                return HlsResolvedMediaSegment(child.playlistUrl, segment)
            }
        }
        return null
    }

    /**
     * Lookahead skip: if inside or approaching an ad range that is safe to jump, return seek target.
     * URL/known-url ads can skip without full cache; fingerprint-only ads still require cached bytes.
     */
    fun resolveAdSkipTarget(
        playlistUrl: String,
        playbackPositionMs: Long,
        leadMs: Long = AD_SKIP_LEAD_MS
    ): HlsAdSkipRange? {
        // Cheap path only — no disk hashing. Call from background thread.
        val ranges = buildAdSkipRangesFast(playlistUrl)
        if (ranges.isEmpty()) return null
        val probe = playbackPositionMs + leadMs.coerceAtLeast(0L)
        val hit = ranges.firstOrNull { range ->
            playbackPositionMs < range.endMs && probe >= range.startMs
        } ?: return null
        if (!isSkipSafe(playlistUrl, hit)) return null
        if (playbackPositionMs >= hit.endMs - AD_SKIP_MIN_FORWARD_MS) return null
        prioritizeResources(hit.segmentUrls)
        return hit
    }

    /**
     * Safe to jump this range: every ad TS is either on disk, or matched by URL/known-url rules
     * (no fingerprint bytes required). Fingerprint-only hits still need cache.
     */
    private fun isSkipSafe(playlistUrl: String, range: HlsAdSkipRange): Boolean {
        val snapshot = findSnapshot(playlistUrl)
        val mediaKey = snapshot?.playlistUrl ?: playlistUrl
        return range.segmentUrls.all { segmentUrl ->
            if (cache.isCached(segmentUrl)) return@all true
            adRuleStore.matchingAdRules(
                playlistUrl = mediaKey,
                segmentUrl = segmentUrl,
                contentFingerprint = null,
                recordHits = false
            ).isNotEmpty()
        }
    }

    private fun prioritizeResources(urls: List<String>) {
        if (urls.isEmpty()) return
        val generation = episodeGeneration.get()
        if (!isGenerationActive(generation)) return
        synchronized(queueLock) {
            val pending = pendingResources.toList()
            if (pending.isEmpty()) return
            val priority = urls.toSet()
            val head = pending.filter { it.url in priority }
            if (head.isEmpty()) return
            val tail = pending.filterNot { it.url in priority }
            pendingResources.clear()
            head.forEach { pendingResources.addLast(it) }
            tail.forEach { pendingResources.addLast(it) }
        }
    }

    /**
     * Continuous cached media from [playbackPositionMs] forward (for UI buffer bar).
     */
    fun prefetchBufferedUntilMs(playlistUrl: String, playbackPositionMs: Long): Long {
        val snapshot = findSnapshot(playlistUrl) ?: return playbackPositionMs
        if (snapshot.segments.isEmpty()) return playbackPositionMs
        val positionSeconds = playbackPositionMs.coerceAtLeast(0L) / 1000.0
        var cursor = positionSeconds
        for (segment in snapshot.segments) {
            val start = segment.startSeconds ?: continue
            val duration = segment.durationSeconds ?: 0.0
            val end = start + duration
            if (end <= positionSeconds) continue
            if (start > cursor + 0.05) break
            if (!cache.isCached(segment.url)) {
                // If this is a known ad segment that is still downloading, stop content buffer here.
                return (cursor * 1000.0).roundToLong()
            }
            cursor = maxOf(cursor, end)
        }
        return (cursor * 1000.0).roundToLong().coerceAtLeast(playbackPositionMs)
    }

    fun markSegmentAsAd(playlistUrl: String, segmentUrl: String) {
        synchronized(snapshotLock) {
            adUrlsByPlaylist.getOrPut(playlistUrl) { mutableSetOf() }.add(normalizeUrl(segmentUrl))
        }
    }

    /**
     * Classify a downloaded/cached segment (URL + global fingerprint) and mark ad timeline.
     * Safe to call from proxy serve path after write-through.
     */
    fun classifyCachedSegment(playlistUrl: String?, segmentUrl: String) {
        val mediaPlaylist = playlistUrl
            ?: synchronized(episodeLock) { activeEpisodeRootUrl }
            ?: return
        classifyDownloadedSegment(mediaPlaylist, segmentUrl)
    }

    /**
     * Re-scan cached segments with global fingerprints (cross-video). Call when approaching ads.
     */
    fun refreshAdMarksWithFingerprints(playlistUrl: String) {
        val snapshot = findSnapshot(playlistUrl) ?: return
        snapshot.segments.forEach { segment ->
            if (cache.isCached(segment.url)) {
                classifyDownloadedSegment(snapshot.playlistUrl, segment.url)
            }
        }
        rebuildAdMarksFromRules(snapshot.playlistUrl)
        if (playlistUrl != snapshot.playlistUrl) {
            rebuildAdMarksFromRules(playlistUrl)
        }
    }

    private fun rebuildAdMarksFromRules(playlistUrl: String) {
        val snapshot = findSnapshot(playlistUrl) ?: return
        val mediaKey = snapshot.playlistUrl
        val urls = snapshot.segments.map { it.url }
        if (urls.isEmpty()) return

        // URL / known-url only. Fingerprints are applied when each TS finishes downloading.
        val matchedUrls = adRuleStore.matchingAdUrls(mediaKey, urls, recordHits = false)

        synchronized(snapshotLock) {
            val set = adUrlsByPlaylist.getOrPut(mediaKey) { mutableSetOf() }
            matchedUrls.forEach { set.add(normalizeUrl(it)) }
            if (playlistUrl != mediaKey) {
                val rootSet = adUrlsByPlaylist.getOrPut(playlistUrl) { mutableSetOf() }
                matchedUrls.forEach { rootSet.add(normalizeUrl(it)) }
            }
            synchronized(episodeLock) {
                activeEpisodeRootUrl?.let { root ->
                    if (root != mediaKey && root != playlistUrl) {
                        val rootSet = adUrlsByPlaylist.getOrPut(root) { mutableSetOf() }
                        matchedUrls.forEach { rootSet.add(normalizeUrl(it)) }
                    }
                }
            }
        }
    }

    /**
     * Fast skip ranges for the playback loop: marked URLs + URL rules only.
     * No SHA-256 of TS bodies (that belongs to download/classify workers).
     */
    private fun buildAdSkipRangesFast(playlistUrl: String): List<HlsAdSkipRange> {
        val snapshot = findSnapshot(playlistUrl) ?: return emptyList()
        if (snapshot.segments.isEmpty()) return emptyList()

        val adUrls = synchronized(snapshotLock) {
            val keys = listOfNotNull(
                snapshot.playlistUrl,
                playlistUrl,
                activeEpisodeRootUrl
            ).distinct()
            keys.flatMap { key -> adUrlsByPlaylist[key].orEmpty() }.toSet()
        }

        val ranges = mutableListOf<HlsAdSkipRange>()
        var runStart: Double? = null
        var runEnd: Double? = null
        val runUrls = mutableListOf<String>()

        fun flush() {
            val start = runStart ?: return
            val end = runEnd ?: return
            if (end > start) {
                ranges.add(
                    HlsAdSkipRange(
                        startMs = (start * 1000.0).roundToLong(),
                        endMs = (end * 1000.0).roundToLong(),
                        segmentUrls = runUrls.toList()
                    )
                )
            }
            runStart = null
            runEnd = null
            runUrls.clear()
        }

        snapshot.segments.forEach { segment ->
            val start = segment.startSeconds ?: return@forEach
            val duration = segment.durationSeconds ?: 0.0
            val end = start + duration
            val isAd = isSegmentAdFast(snapshot.playlistUrl, segment.url, adUrls)
            if (isAd) {
                if (runStart == null) runStart = start
                runEnd = end
                runUrls.add(segment.url)
            } else {
                flush()
            }
        }
        flush()
        return ranges
    }

    private fun isSegmentAdFast(
        playlistUrl: String,
        segmentUrl: String,
        markedAdUrls: Set<String>
    ): Boolean {
        if (normalizeUrl(segmentUrl) in markedAdUrls) return true
        return adRuleStore.matchingAdRules(playlistUrl, segmentUrl, null, false).isNotEmpty()
    }

    private fun findSnapshot(playlistUrl: String): HlsPlaylistSnapshot? {
        val snapshots = synchronized(snapshotLock) { playlistSnapshots.toMap() }
        snapshots[playlistUrl]?.let { direct ->
            if (direct.segments.isNotEmpty()) return direct
            // Master: pick first child media snapshot with segments.
            direct.childManifestUrls.forEach { child ->
                snapshots[child]?.takeIf { it.segments.isNotEmpty() }?.let { return it }
            }
        }
        // Any media snapshot registered as child of this root.
        snapshots.values.forEach { snap ->
            if (snap.segments.isNotEmpty() && playlistUrl in snap.childManifestUrls) {
                // playlistUrl is a child listed by snap — wrong direction; skip
            }
        }
        snapshots.values.forEach { parent ->
            if (playlistUrl == parent.playlistUrl) return@forEach
            if (parent.childManifestUrls.any { it == playlistUrl || normalizeUrl(it) == normalizeUrl(playlistUrl) }) {
                snapshots[playlistUrl]?.takeIf { it.segments.isNotEmpty() }?.let { return it }
            }
        }
        // Fallback: newest media snapshot with segments (single-episode playback).
        return snapshots.values
            .filter { it.segments.isNotEmpty() }
            .maxByOrNull { it.updatedAtMs }
    }

    private fun precachePlaylistTree(playlistUrl: String, generation: Long, depth: Int) {
        if (!isGenerationActive(generation) || depth > MAX_PLAYLIST_DEPTH) return
        val body = downloadPlaylistText(playlistUrl) ?: return
        if (!isGenerationActive(generation)) return
        runCatching { cache.writeText(playlistUrl, HLS_CONTENT_TYPE, body) }
        recordManifestSnapshot(playlistUrl, body)

        val childManifests = manifestRewriter.extractChildManifestUrls(playlistUrl, body)
        val mediaSegments = manifestRewriter.extractMediaSegments(playlistUrl, body)
        if (mediaSegments.isEmpty() && childManifests.isNotEmpty()) {
            precachePlaylistTree(selectMediaVariant(childManifests), generation, depth + 1)
            return
        }
        if (mediaSegments.isEmpty()) return

        val resourceUrls = collectAllResourceUrls(playlistUrl, body).let { urls ->
            if (body.contains(ENDLIST_TAG, ignoreCase = true)) urls
            else urls.take(LIVE_WINDOW_RESOURCE_LIMIT)
        }
        // Sequential full download including ads.
        enqueueResources(resourceUrls, generation, playlistUrl = playlistUrl)
    }

    private fun collectAllResourceUrls(playlistUrl: String, playlist: String): List<String> {
        val rewritten = manifestRewriter.rewrite(
            playlistUrl = playlistUrl,
            playlist = playlist,
            knownAdResourceUrls = emptySet()
        ) { url, _ -> url }
        return rewritten.prefetchUrls
    }

    private fun selectMediaVariant(childManifestUrls: List<String>): String {
        if (childManifestUrls.isEmpty()) error("empty variants")
        if (childManifestUrls.size == 1) return childManifestUrls.first()
        return childManifestUrls[childManifestUrls.size / 2]
    }

    private fun downloadPlaylistText(url: String): String? {
        cache.cachedText(url)?.takeIf { it.isNotBlank() }?.let { return it }
        val request = Request.Builder().url(url).get().build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }

    private fun enqueueResources(
        urls: List<String>,
        generation: Long,
        playlistUrl: String? = null
    ) {
        if (urls.isEmpty() || !isGenerationActive(generation)) return
        synchronized(queueLock) {
            urls.forEach { url ->
                if (cache.isCached(url)) {
                    // Already on disk — still classify fingerprint if possible.
                    playlistUrl?.let { classifyDownloadedSegment(it, url) }
                    return@forEach
                }
                if (queuedResources.contains(url) || inFlight.contains(url)) return@forEach
                if (pendingResources.size >= FULL_EPISODE_QUEUE_LIMIT) return@forEach
                pendingResources.addLast(
                    QueuedResource(url = url, generation = generation, playlistUrl = playlistUrl)
                )
                queuedResources.add(url)
            }
            if (!resourceWorkerRunning && pendingResources.isNotEmpty()) {
                resourceWorkerRunning = true
                scope.launch { drainResourceQueue() }
            }
        }
    }

    private fun clearPendingQueue() {
        synchronized(queueLock) {
            pendingResources.clear()
            queuedResources.clear()
        }
    }

    private fun clearAdMarks() {
        synchronized(snapshotLock) {
            adUrlsByPlaylist.clear()
        }
    }

    private fun isGenerationActive(generation: Long): Boolean {
        return generation == 0L || generation == episodeGeneration.get()
    }

    private suspend fun drainResourceQueue() {
        while (true) {
            val item = synchronized(queueLock) {
                while (pendingResources.isNotEmpty()) {
                    val next = pendingResources.removeFirst()
                    queuedResources.remove(next.url)
                    if (isGenerationActive(next.generation)) {
                        return@synchronized next
                    }
                }
                null
            } ?: break

            if (cache.isCached(item.url)) {
                item.playlistUrl?.let { classifyDownloadedSegment(it, item.url) }
                continue
            }
            if (!inFlight.add(item.url)) continue
            runCatching {
                val request = Request.Builder().url(item.url).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body ?: return@use
                    cache.writeFromStream(
                        url = item.url,
                        contentType = body.contentType()?.toString(),
                        input = body.byteStream()
                    )
                }
                item.playlistUrl?.let { classifyDownloadedSegment(it, item.url) }
            }.onFailure { error ->
                Log.d(TAG, "prefetch resource failed url=${item.url} error=${error.message}")
            }
            inFlight.remove(item.url)
            // Yield so playback HTTP is not starved by full-episode precache.
            delay(PRECACHE_ITEM_YIELD_MS)
        }
        synchronized(queueLock) {
            resourceWorkerRunning = false
            if (pendingResources.isNotEmpty()) {
                resourceWorkerRunning = true
                scope.launch { drainResourceQueue() }
            }
        }
    }

    private fun classifyDownloadedSegment(playlistUrl: String, segmentUrl: String) {
        val fingerprint = cache.contentFingerprint(segmentUrl)
        val matched = adRuleStore.matchingAdRules(
            playlistUrl = playlistUrl,
            segmentUrl = segmentUrl,
            contentFingerprint = fingerprint,
            recordHits = false
        )
        if (matched.isNotEmpty()) {
            markSegmentAsAd(playlistUrl, segmentUrl)
            // Also mark under active episode root so master-url lookups see the range.
            synchronized(episodeLock) {
                activeEpisodeRootUrl?.let { root ->
                    if (root != playlistUrl) markSegmentAsAd(root, segmentUrl)
                }
            }
            val rule = matched.first()
            if (rule.matchScope == HlsAdMatchScope.GlobalFingerprint || fingerprint != null) {
                adRuleStore.rememberKnownAdUrl(segmentUrl, rule, fingerprint)
            }
        }
    }

    private fun normalizeUrl(url: String): String = url.trim().substringBefore('?')

    private fun HlsPlaylistSnapshot.findSegment(playbackPositionMs: Long): HlsMediaSegment? {
        if (segments.isEmpty() || playbackPositionMs < 0L) return null
        val positionSeconds = playbackPositionMs / 1000.0
        return segments.firstOrNull { segment ->
            val start = segment.startSeconds ?: return@firstOrNull false
            val end = start + (segment.durationSeconds ?: 0.0)
            positionSeconds >= start && positionSeconds < end
        } ?: segments.lastOrNull { segment ->
            val start = segment.startSeconds ?: return@lastOrNull false
            positionSeconds >= start
        }
    }

    private data class QueuedResource(
        val url: String,
        val generation: Long,
        val playlistUrl: String?
    )

    private data class HlsPlaylistSnapshot(
        val playlistUrl: String,
        val segments: List<HlsMediaSegment>,
        val childManifestUrls: List<String>,
        val updatedAtMs: Long
    )

    private companion object {
        private const val TAG = "iCinemaHlsPrefetch"
        private const val HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl"
        private const val ENDLIST_TAG = "#EXT-X-ENDLIST"
        private const val INITIAL_RESOURCE_PREFETCH_LIMIT = 6
        private const val RESOURCE_PREFETCH_LIMIT = 24
        private const val FULL_EPISODE_QUEUE_LIMIT = 2048
        private const val FULL_EPISODE_PRECACHE_DELAY_MS = 1_500L
        private const val LIVE_WINDOW_RESOURCE_LIMIT = 40
        private const val MAX_PLAYLIST_DEPTH = 2
        private const val SNAPSHOT_LIMIT = 32
        /** Seek well before ad start so decoder never renders ad frames. */
        private const val AD_SKIP_LEAD_MS = 3_500L
        private const val AD_SKIP_MIN_FORWARD_MS = 250L
        private const val PRECACHE_ITEM_YIELD_MS = 80L
    }
}
