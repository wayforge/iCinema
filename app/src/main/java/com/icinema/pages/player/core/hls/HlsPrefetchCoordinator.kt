package com.icinema.pages.player.core.hls

import android.util.Log
import java.util.ArrayDeque
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class HlsPrefetchCoordinator @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cache: HlsPersistentCache,
    private val manifestRewriter: HlsManifestRewriter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = Collections.synchronizedSet(mutableSetOf<String>())
    private val manifestPolicyLock = Any()
    private val manifestResourcePrefetch = mutableMapOf<String, Boolean>()
    private val queueLock = Any()
    private val pendingResources = ArrayDeque<String>()
    private val queuedResources = mutableSetOf<String>()
    private var resourceWorkerRunning = false
    private val snapshotLock = Any()
    private val playlistSnapshots = LinkedHashMap<String, HlsPlaylistSnapshot>()

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
                    val request = Request.Builder().url(manifestUrl).get().build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body?.string().orEmpty()
                        if (body.isBlank()) return@use
                        runCatching { cache.writeText(manifestUrl, HLS_CONTENT_TYPE, body) }
                        recordManifestSnapshot(manifestUrl, body)
                        val includeResources = synchronized(manifestPolicyLock) {
                            manifestResourcePrefetch[manifestUrl] == true
                        }
                        if (includeResources) {
                            val result = manifestRewriter.rewrite(manifestUrl, body) { url, _ -> url }
                            prefetchResources(result.prefetchUrls.take(INITIAL_RESOURCE_PREFETCH_LIMIT))
                        }
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

    fun prefetchResources(resourceUrls: List<String>) {
        synchronized(queueLock) {
            resourceUrls
                .distinct()
                .take(RESOURCE_PREFETCH_LIMIT)
                .forEach { url ->
                    if (cache.isCached(url) || queuedResources.contains(url) || inFlight.contains(url)) {
                        return@forEach
                    }
                    if (pendingResources.size >= RESOURCE_QUEUE_LIMIT) {
                        return@forEach
                    }
                    pendingResources.addLast(url)
                    queuedResources.add(url)
                }
            if (!resourceWorkerRunning && pendingResources.isNotEmpty()) {
                resourceWorkerRunning = true
                scope.launch { drainResourceQueue() }
            }
        }
    }

    fun recordManifestSnapshot(manifestUrl: String, playlist: String) {
        val snapshot = HlsPlaylistSnapshot(
            playlistUrl = manifestUrl,
            segments = manifestRewriter.extractMediaSegments(manifestUrl, playlist),
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
    }

    fun resolveSegment(playlistUrl: String, playbackPositionMs: Long): HlsResolvedMediaSegment? {
        val snapshots = synchronized(snapshotLock) {
            playlistSnapshots.values.toList()
        }
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

    private fun drainResourceQueue() {
        while (true) {
            val url = synchronized(queueLock) {
                if (pendingResources.isEmpty()) {
                    null
                } else {
                    pendingResources.removeFirst().also { queuedResources.remove(it) }
                }
            } ?: break

            if (cache.isCached(url) || !inFlight.add(url)) continue
            runCatching {
                val request = Request.Builder().url(url).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body ?: return@use
                    cache.writeFromStream(
                        url = url,
                        contentType = body.contentType()?.toString(),
                        input = body.byteStream()
                    )
                }
            }.onFailure { error ->
                Log.d(TAG, "prefetch resource failed url=$url error=${error.message}")
            }
            inFlight.remove(url)
        }
        synchronized(queueLock) {
            resourceWorkerRunning = false
            if (pendingResources.isNotEmpty()) {
                resourceWorkerRunning = true
                scope.launch { drainResourceQueue() }
            }
        }
    }

    private fun HlsPlaylistSnapshot.findSegment(playbackPositionMs: Long): HlsMediaSegment? {
        if (segments.isEmpty() || playbackPositionMs <= 0L) return null
        val positionSeconds = playbackPositionMs / 1000.0
        return segments.firstOrNull { segment ->
            val start = segment.startSeconds ?: return@firstOrNull false
            val end = start + (segment.durationSeconds ?: 0.0)
            positionSeconds >= start && positionSeconds < end
        }
    }

    private data class HlsPlaylistSnapshot(
        val playlistUrl: String,
        val segments: List<HlsMediaSegment>,
        val childManifestUrls: List<String>,
        val updatedAtMs: Long
    )

    private companion object {
        private const val TAG = "iCinemaHlsPrefetch"
        private const val HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl"
        private const val INITIAL_RESOURCE_PREFETCH_LIMIT = 6
        private const val RESOURCE_PREFETCH_LIMIT = 24
        private const val RESOURCE_QUEUE_LIMIT = 64
        private const val SNAPSHOT_LIMIT = 32
    }
}
