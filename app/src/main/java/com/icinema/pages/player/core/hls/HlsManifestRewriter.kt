package com.icinema.pages.player.core.hls

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class HlsManifestRewriter @Inject constructor() {
    fun rewrite(
        playlistUrl: String,
        playlist: String,
        proxyUrlFactory: (String, HlsProxyResourceType) -> String
    ): HlsRewriteResult {
        val output = mutableListOf<String>()
        val pendingSegmentTags = mutableListOf<String>()
        val prefetchUrls = linkedSetOf<String>()
        var nextUriIsVariant = false
        var inAdBreak = false

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
                    if (!inAdBreak) {
                        flushPendingSegmentTags()
                        output.add(rawLine)
                    }
                }

                isAdBreakStart(line) -> {
                    inAdBreak = true
                    pendingSegmentTags.clear()
                }

                isAdBreakEnd(line) -> {
                    inAdBreak = false
                    pendingSegmentTags.clear()
                }

                isAdMarker(line) -> {
                    pendingSegmentTags.clear()
                }

                line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) -> {
                    if (!inAdBreak) {
                        flushPendingSegmentTags()
                        output.add(rawLine)
                        nextUriIsVariant = true
                    }
                }

                line.startsWith("#EXT-X-I-FRAME-STREAM-INF", ignoreCase = true) -> {
                    if (!inAdBreak) {
                        flushPendingSegmentTags()
                        output.add(rewriteUriAttribute(playlistUrl, rawLine, HlsProxyResourceType.Manifest, proxyUrlFactory).line)
                    }
                }

                line.startsWith("#EXT-X-MEDIA", ignoreCase = true) -> {
                    if (!inAdBreak) {
                        flushPendingSegmentTags()
                        output.add(rewriteUriAttribute(playlistUrl, rawLine, HlsProxyResourceType.Manifest, proxyUrlFactory).line)
                    }
                }

                line.startsWith("#EXT-X-KEY", ignoreCase = true) ||
                    line.startsWith("#EXT-X-MAP", ignoreCase = true) -> {
                    if (!inAdBreak) {
                        val rewritten = rewriteUriAttribute(
                            playlistUrl = playlistUrl,
                            line = rawLine,
                            resourceType = HlsProxyResourceType.Resource,
                            proxyUrlFactory = proxyUrlFactory
                        )
                        rewritten.absoluteUrl?.let(prefetchUrls::add)
                        pendingSegmentTags.add(rewritten.line)
                    }
                }

                line.startsWith("#EXT", ignoreCase = true) -> {
                    if (isSegmentScopedTag(line)) {
                        if (!inAdBreak) {
                            pendingSegmentTags.add(rawLine)
                        } else {
                            pendingSegmentTags.clear()
                        }
                    } else if (!inAdBreak) {
                        flushPendingSegmentTags()
                        output.add(rawLine)
                    }
                }

                line.startsWith("#") -> {
                    if (!inAdBreak) {
                        pendingSegmentTags.add(rawLine)
                    }
                }

                else -> {
                    val absoluteUrl = resolveUrl(playlistUrl, line)
                    if (inAdBreak) {
                        pendingSegmentTags.clear()
                        nextUriIsVariant = false
                        return@forEach
                    }

                    val resourceType = if (
                        nextUriIsVariant ||
                        absoluteUrl.contains(".m3u8", ignoreCase = true)
                    ) {
                        HlsProxyResourceType.Manifest
                    } else {
                        HlsProxyResourceType.Resource
                    }
                    flushPendingSegmentTags()
                    output.add(proxyUrlFactory(absoluteUrl, resourceType))
                    if (resourceType == HlsProxyResourceType.Resource) {
                        prefetchUrls.add(absoluteUrl)
                    }
                    nextUriIsVariant = false
                }
            }
        }

        flushPendingSegmentTags()
        return HlsRewriteResult(
            playlist = output.joinToString(separator = "\n"),
            prefetchUrls = prefetchUrls.toList()
        )
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

    private fun resolveUrl(baseUrl: String, candidate: String): String {
        return baseUrl.toHttpUrlOrNull()
            ?.resolve(candidate)
            ?.toString()
            ?: java.net.URI(baseUrl).resolve(candidate).toString()
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
        return line.startsWith("#EXT-X-CUE-OUT", ignoreCase = true) ||
            line.startsWith("#EXT-X-SPLICEPOINT-SCTE35", ignoreCase = true)
    }

    private fun isAdBreakEnd(line: String): Boolean {
        return line.startsWith("#EXT-X-CUE-IN", ignoreCase = true)
    }

    private fun isAdMarker(line: String): Boolean {
        return line.startsWith("#EXT-OATCLS-SCTE35", ignoreCase = true) ||
            line.startsWith("#EXT-X-ASSET", ignoreCase = true) ||
            line.startsWith("#EXT-X-DATERANGE", ignoreCase = true) &&
            AD_DATERANGE_REGEX.containsMatchIn(line)
    }

    private data class RewrittenAttribute(
        val line: String,
        val absoluteUrl: String?
    )

    private companion object {
        private val URI_ATTRIBUTE_REGEX = Regex("""URI="([^"]+)"""")
        private val AD_DATERANGE_REGEX = Regex("""(?i)(CLASS="[^"]*ad[^"]*"|SCTE35-)""")
    }
}
