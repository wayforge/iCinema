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
    val updatedAtMs: Long,
    val hitCount: Long = 0L,
    val lastHitAtMs: Long? = null,
    val contentSha256: String? = null,
    val contentLength: Long? = null
)

data class HlsAdRuleValidation(
    val playlistMatches: Boolean,
    val segmentMatches: Boolean
) {
    val matches: Boolean
        get() = playlistMatches && segmentMatches
}

data class MarkedHlsAdSegment(
    val rule: HlsAdRule,
    val message: String,
    val segmentEndPositionMs: Long?
)

data class HlsAdDetectionCandidate(
    val rule: HlsAdRule,
    val playlistUrl: String,
    val segmentUrl: String,
    val segmentStartPositionMs: Long?,
    val segmentEndPositionMs: Long?,
    val contentFingerprint: HlsContentFingerprint?
)

data class HlsContentFingerprint(
    val sha256: String,
    val length: Long
)

data class HlsDetectedAdSegment(
    val id: String,
    val ruleId: String,
    val playlistUrl: String,
    val segmentUrl: String,
    val segmentStartPositionMs: Long?,
    val segmentEndPositionMs: Long?,
    val durationSeconds: Double?,
    val contentSha256: String?,
    val contentLength: Long?,
    val videoTitle: String,
    val episodeTitle: String,
    val detectedBy: String,
    val detectedCount: Long,
    val firstDetectedAtMs: Long,
    val lastDetectedAtMs: Long
)
