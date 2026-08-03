package com.icinema.pages.player.core.hls

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsSessionManager @Inject constructor(
    private val proxyServer: HlsProxyServer,
    private val prefetchCoordinator: HlsPrefetchCoordinator,
    private val adRuleStore: HlsAdRuleStore
) {
    fun preparePlaybackUrl(originUrl: String): String {
        prefetchCoordinator.prefetchManifest(originUrl, includeInitialResources = true)
        return proxyServer.proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Manifest,
            target = HlsProxyTarget.Loopback
        )
    }

    fun prepareCastUrl(originUrl: String): String {
        prefetchCoordinator.prefetchManifest(originUrl, includeInitialResources = true)
        return proxyServer.proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Manifest,
            target = HlsProxyTarget.Lan
        )
    }

    fun resourcePlaybackUrl(originUrl: String): String {
        return proxyServer.resourceUrl(originUrl, HlsProxyTarget.Loopback)
    }

    fun prefetchNextEpisodeManifest(originUrl: String) {
        prefetchCoordinator.prefetchManifest(originUrl, includeInitialResources = false)
    }

    fun markCurrentSegmentAsAd(
        originUrl: String,
        playbackPositionMs: Long,
        videoTitle: String,
        episodeTitle: String
    ): Result<MarkedHlsAdSegment> {
        return proxyServer.markCurrentSegmentAsAd(
            originUrl = originUrl,
            playbackPositionMs = playbackPositionMs,
            videoTitle = videoTitle,
            episodeTitle = episodeTitle
        )
    }

    fun resolveAdDetectionCandidate(
        originUrl: String,
        playbackPositionMs: Long,
        recordHit: Boolean = false
    ): HlsAdDetectionCandidate? {
        return proxyServer.resolveAdDetectionCandidate(
            originUrl = originUrl,
            playbackPositionMs = playbackPositionMs,
            recordHit = recordHit
        )
    }

    fun recordDetectedSegment(
        candidate: HlsAdDetectionCandidate,
        videoTitle: String,
        episodeTitle: String
    ) {
        val durationSeconds = candidate.segmentStartPositionMs
            ?.let { start -> candidate.segmentEndPositionMs?.minus(start) }
            ?.takeIf { it > 0L }
            ?.div(1000.0)
            ?: candidate.rule.durationSeconds
        adRuleStore.recordDetectedSegment(
            rule = candidate.rule,
            playlistUrl = candidate.playlistUrl,
            segmentUrl = candidate.segmentUrl,
            segmentStartPositionMs = candidate.segmentStartPositionMs,
            segmentEndPositionMs = candidate.segmentEndPositionMs,
            durationSeconds = durationSeconds,
            contentFingerprint = candidate.contentFingerprint,
            videoTitle = videoTitle,
            episodeTitle = episodeTitle,
            detectedBy = if (candidate.contentFingerprint != null && candidate.rule.contentSha256 == candidate.contentFingerprint.sha256) {
                "内容指纹"
            } else {
                "规则匹配"
            }
        )
    }
}
