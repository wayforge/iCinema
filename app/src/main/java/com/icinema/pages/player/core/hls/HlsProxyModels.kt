package com.icinema.pages.player.core.hls

enum class HlsProxyTarget {
    Loopback,
    Lan
}

enum class HlsProxyResourceType {
    Manifest,
    Resource
}

data class HlsRewriteResult(
    val playlist: String,
    val prefetchUrls: List<String>
)

data class HlsMediaSegment(
    val url: String,
    val durationSeconds: Double?,
    val startSeconds: Double?
)

data class HlsResolvedMediaSegment(
    val playlistUrl: String,
    val segment: HlsMediaSegment
)

data class HlsAdRule(
    val id: String,
    val playlistUrl: String,
    val segmentUrl: String,
    val urlPattern: String?,
    val matchText: String,
    val durationSeconds: Double?,
    val videoTitle: String,
    val episodeTitle: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

data class MarkedHlsAdSegment(
    val rule: HlsAdRule,
    val message: String
)
