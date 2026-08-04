package com.icinema.pages.player.core.hls

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class HlsManifestRewriter @Inject constructor() {
    fun rewrite(
        playlistUrl: String,
        playlist: String,
        knownAdResourceUrls: Set<String> = emptySet(),
        proxyUrlFactory: (String, HlsProxyResourceType) -> String
    ): HlsRewriteResult {
        val output = mutableListOf<String>()
        val pendingSegmentTags = mutableListOf<String>()
        val prefetchUrls = linkedSetOf<String>()
        var nextUriIsVariant = false
        var droppedAdSinceLastMedia = false
        var emittedMediaSegment = false

        fun flushPendingSegmentTags() {
            if (pendingSegmentTags.isNotEmpty()) {
                output.addAll(pendingSegmentTags)
                pendingSegmentTags.clear()
            }
        }

        playlist.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> {
                    // Keep blanks only outside pending segment blocks to avoid flushing KEY early.
                    if (pendingSegmentTags.isEmpty()) {
                        output.add(rawLine)
                    }
                }

                line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                    flushPendingSegmentTags()
                    output.add(rawLine)
                    nextUriIsVariant = true
                }

                line.startsWith("#EXT-X-I-FRAME-STREAM-INF", ignoreCase = true) -> {
                    flushPendingSegmentTags()
                    output.add(rewriteUriAttribute(playlistUrl, rawLine, HlsProxyResourceType.Manifest, proxyUrlFactory).line)
                }

                line.startsWith("#EXT-X-MEDIA", ignoreCase = true) -> {
                    flushPendingSegmentTags()
                    output.add(rewriteUriAttribute(playlistUrl, rawLine, HlsProxyResourceType.Manifest, proxyUrlFactory).line)
                }

                line.startsWith("#EXT-X-KEY", ignoreCase = true) ||
                    line.startsWith("#EXT-X-MAP", ignoreCase = true) -> {
                    val rewritten = rewriteUriAttribute(
                        playlistUrl = playlistUrl,
                        line = rawLine,
                        resourceType = HlsProxyResourceType.Resource,
                        proxyUrlFactory = proxyUrlFactory
                    )
                    rewritten.absoluteUrl?.let(prefetchUrls::add)
                    pendingSegmentTags.add(rewritten.line)
                }

                // Drop ad-break markers that only wrap removed ads; they confuse some players.
                isAdBreakStart(line) || isAdBreakEnd(line) || isAdMarker(line) -> {
                    if (knownAdResourceUrls.isEmpty()) {
                        if (isSegmentScopedTag(line)) {
                            pendingSegmentTags.add(rawLine)
                        } else {
                            flushPendingSegmentTags()
                            output.add(rawLine)
                        }
                    }
                    // When stripping ads, omit cue/ad markers from output.
                }

                line.startsWith("#EXT", ignoreCase = true) -> {
                    if (isSegmentScopedTag(line)) {
                        pendingSegmentTags.add(rawLine)
                    } else {
                        flushPendingSegmentTags()
                        output.add(rawLine)
                    }
                }

                line.startsWith("#") -> {
                    pendingSegmentTags.add(rawLine)
                }

                else -> {
                    val absoluteUrl = resolveUrl(playlistUrl, line)
                    val resourceType = if (
                        nextUriIsVariant ||
                        absoluteUrl.contains(".m3u8", ignoreCase = true)
                    ) {
                        HlsProxyResourceType.Manifest
                    } else {
                        HlsProxyResourceType.Resource
                    }

                    if (
                        resourceType == HlsProxyResourceType.Resource &&
                        isKnownAdResource(absoluteUrl, knownAdResourceUrls)
                    ) {
                        // Drop this media segment and its scoped tags; keep KEY/MAP for later segments.
                        pendingSegmentTags.retainSharedSegmentTags()
                        droppedAdSinceLastMedia = true
                        nextUriIsVariant = false
                    } else {
                        if (
                            resourceType == HlsProxyResourceType.Resource &&
                            droppedAdSinceLastMedia
                        ) {
                            // ExoPlayer needs an explicit discontinuity after removed ad media.
                            val alreadyHasDiscontinuity = pendingSegmentTags.any {
                                it.trim().startsWith("#EXT-X-DISCONTINUITY", ignoreCase = true)
                            }
                            if (!alreadyHasDiscontinuity) {
                                pendingSegmentTags.add(0, "#EXT-X-DISCONTINUITY")
                            }
                            droppedAdSinceLastMedia = false
                        }
                        flushPendingSegmentTags()
                        output.add(proxyUrlFactory(absoluteUrl, resourceType))
                        if (resourceType == HlsProxyResourceType.Resource) {
                            prefetchUrls.add(absoluteUrl)
                            emittedMediaSegment = true
                        }
                        nextUriIsVariant = false
                    }
                }
            }
        }

        flushPendingSegmentTags()
        return HlsRewriteResult(
            playlist = output.joinToString(separator = "\n"),
            prefetchUrls = prefetchUrls.toList()
        )
    }

    fun extractAdResourceUrls(playlistUrl: String, playlist: String): Set<String> {
        val urls = linkedSetOf<String>()
        var nextUriIsVariant = false
        var inAdBreak = false
        var skipNextResource = false

        playlist.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> Unit
                isAdBreakStart(line) -> {
                    inAdBreak = true
                    skipNextResource = true
                }
                isAdBreakEnd(line) -> {
                    inAdBreak = false
                    skipNextResource = false
                }
                isAdMarker(line) -> {
                    skipNextResource = true
                }
                line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                    nextUriIsVariant = true
                }
                line.startsWith("#") -> Unit
                else -> {
                    val absoluteUrl = resolveUrl(playlistUrl, line)
                    val isManifest = nextUriIsVariant || absoluteUrl.contains(".m3u8", ignoreCase = true)
                    if (!isManifest && (inAdBreak || skipNextResource || isLikelyAdResourceUrl(absoluteUrl))) {
                        urls.add(absoluteUrl)
                    }
                    nextUriIsVariant = false
                    skipNextResource = false
                }
            }
        }

        return urls
    }

    fun extractMediaSegments(playlistUrl: String, playlist: String): List<HlsMediaSegment> {
        val segments = mutableListOf<HlsMediaSegment>()
        var nextUriIsVariant = false
        var pendingDurationSeconds: Double? = null
        var elapsedSeconds = 0.0

        playlist.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                    nextUriIsVariant = true
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingDurationSeconds = line.substringAfter(':', "")
                        .substringBefore(',')
                        .toDoubleOrNull()
                }
                line.isBlank() || line.startsWith("#") -> Unit
                else -> {
                    val absoluteUrl = resolveUrl(playlistUrl, line)
                    val isManifest = nextUriIsVariant || absoluteUrl.contains(".m3u8", ignoreCase = true)
                    if (!isManifest) {
                        val duration = pendingDurationSeconds
                        segments.add(
                            HlsMediaSegment(
                                url = absoluteUrl,
                                durationSeconds = duration,
                                startSeconds = elapsedSeconds
                            )
                        )
                        elapsedSeconds += duration ?: 0.0
                    }
                    nextUriIsVariant = false
                    pendingDurationSeconds = null
                }
            }
        }

        return segments
    }

    fun extractChildManifestUrls(playlistUrl: String, playlist: String): List<String> {
        val urls = linkedSetOf<String>()
        var nextUriIsVariant = false

        playlist.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> Unit
                line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                    nextUriIsVariant = true
                }
                line.startsWith("#EXT-X-I-FRAME-STREAM-INF", ignoreCase = true) ||
                    line.startsWith("#EXT-X-MEDIA", ignoreCase = true) -> {
                    extractUriAttributeUrl(playlistUrl, line)?.let(urls::add)
                }
                line.startsWith("#") -> Unit
                else -> {
                    val absoluteUrl = resolveUrl(playlistUrl, line)
                    if (nextUriIsVariant || absoluteUrl.contains(".m3u8", ignoreCase = true)) {
                        urls.add(absoluteUrl)
                    }
                    nextUriIsVariant = false
                }
            }
        }

        return urls.toList()
    }

    private fun rewriteUriAttribute(
        playlistUrl: String,
        line: String,
        resourceType: HlsProxyResourceType,
        proxyUrlFactory: (String, HlsProxyResourceType) -> String
    ): RewrittenAttribute {
        val match = URI_ATTRIBUTE_REGEX.find(line) ?: return RewrittenAttribute(line, null)
        val rawUri = match.groupValues[1]
        val absoluteUrl = resolveUrl(playlistUrl, rawUri)
        val proxiedUrl = proxyUrlFactory(absoluteUrl, resourceType)
        val rewrittenLine = line.replaceRange(match.groups[1]!!.range, proxiedUrl)
        return RewrittenAttribute(rewrittenLine, absoluteUrl)
    }

    private fun extractUriAttributeUrl(playlistUrl: String, line: String): String? {
        val match = URI_ATTRIBUTE_REGEX.find(line) ?: return null
        return resolveUrl(playlistUrl, match.groupValues[1])
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String {
        return baseUrl.toHttpUrlOrNull()
            ?.resolve(candidate)
            ?.toString()
            ?: java.net.URI(baseUrl).resolve(candidate).toString()
    }

    private fun MutableList<String>.retainSharedSegmentTags() {
        val shared = filter { line ->
            val trimmed = line.trim()
            trimmed.startsWith("#EXT-X-KEY", ignoreCase = true) ||
                trimmed.startsWith("#EXT-X-MAP", ignoreCase = true)
        }
        clear()
        addAll(shared)
    }

    private fun isKnownAdResource(url: String, knownAdResourceUrls: Set<String>): Boolean {
        if (knownAdResourceUrls.isEmpty()) return false
        if (url in knownAdResourceUrls) return true
        // Only exact / ignore-query match. Bare filename match is too aggressive and can drop content TS.
        val normalized = url.substringBefore('?')
        return knownAdResourceUrls.any { it.substringBefore('?') == normalized }
    }

    private fun isSegmentScopedTag(line: String): Boolean {
        return line.startsWith("#EXTINF", ignoreCase = true) ||
            line.startsWith("#EXT-X-BYTERANGE", ignoreCase = true) ||
            line.startsWith("#EXT-X-PROGRAM-DATE-TIME", ignoreCase = true) ||
            line.startsWith("#EXT-X-DISCONTINUITY", ignoreCase = true) ||
            line.startsWith("#EXT-X-CUE-OUT-CONT", ignoreCase = true) ||
            line.startsWith("#EXT-OATCLS-SCTE35", ignoreCase = true) ||
            line.startsWith("#EXT-X-ASSET", ignoreCase = true)
    }

    private fun isAdBreakStart(line: String): Boolean {
        return line.equals("#EXT-X-CUE-OUT", ignoreCase = true) ||
            line.startsWith("#EXT-X-CUE-OUT:", ignoreCase = true) ||
            line.startsWith("#EXT-X-SPLICEPOINT-SCTE35", ignoreCase = true)
    }

    private fun isAdBreakEnd(line: String): Boolean {
        return line.startsWith("#EXT-X-CUE-IN", ignoreCase = true)
    }

    private fun isAdMarker(line: String): Boolean {
        return line.startsWith("#EXT-OATCLS-SCTE35", ignoreCase = true) ||
            line.startsWith("#EXT-X-ASSET", ignoreCase = true) ||
            line.startsWith("#EXT-X-SCTE35", ignoreCase = true) ||
            line.startsWith("#EXT-X-CUE-OUT-CONT", ignoreCase = true) ||
            line.startsWith("#EXT-X-VMAP-AD-BREAK", ignoreCase = true) ||
            line.startsWith("#EXT-X-PLACEMENT-OPPORTUNITY", ignoreCase = true) ||
            line.startsWith("#EXT-X-DATERANGE", ignoreCase = true) &&
            AD_DATERANGE_REGEX.containsMatchIn(line)
    }

    private fun isLikelyAdResourceUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return AD_RESOURCE_URL_REGEX.containsMatchIn(lowerUrl)
    }

    private data class RewrittenAttribute(
        val line: String,
        val absoluteUrl: String?
    )

    private companion object {
        private val URI_ATTRIBUTE_REGEX = Regex("""URI="([^"]+)"""")
        private val AD_DATERANGE_REGEX = Regex("""(?i)(CLASS="[^"]*(ad|advert|preroll|midroll|postroll)[^"]*"|SCTE35-|X-COM-|CUE=)""")
        private val AD_RESOURCE_URL_REGEX = Regex(
            """(?i)(^|[/?&_.=-])(ad|ads|adv|advert|advertise|advertisement|vast|vmap|preroll|midroll|postroll|sponsor|commercial)([/?&_.=-]|$)"""
        )
    }
}
