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
        prefetchCoordinator.startEpisodePrecache(originUrl)
        return proxyServer.proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Manifest,
            target = HlsProxyTarget.Loopback
        )
    }

    fun prepareCastUrl(originUrl: String): String {
        prefetchCoordinator.startEpisodePrecache(originUrl)
        return proxyServer.proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Manifest,
            target = HlsProxyTarget.Lan
        )
    }

    fun resourcePlaybackUrl(originUrl: String): String {
        return proxyServer.resourceUrl(originUrl, HlsProxyTarget.Loopback)
    }

    /** Next episode: warm m3u8 text only (no full segment cache). */
    fun prefetchNextEpisodeManifest(originUrl: String) {
        prefetchCoordinator.prefetchManifest(originUrl, includeInitialResources = false)
    }

    fun cancelEpisodePrecache() {
        prefetchCoordinator.cancelEpisodePrecache()
    }

    fun resolveAdSkipTarget(originUrl: String, playbackPositionMs: Long): HlsAdSkipRange? {
        // Range build already fingerprint-classifies unmarked cached segments (light path).
        return prefetchCoordinator.resolveAdSkipTarget(originUrl, playbackPositionMs)
    }

    fun prefetchBufferedUntilMs(originUrl: String, playbackPositionMs: Long): Long {
        return prefetchCoordinator.prefetchBufferedUntilMs(originUrl, playbackPositionMs)
    }

    fun classifyCachedSegment(playlistUrl: String?, segmentUrl: String) {
        prefetchCoordinator.classifyCachedSegment(playlistUrl, segmentUrl)
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
            detectedBy = when {
                candidate.contentFingerprint != null &&
                    candidate.rule.contentSha256 == candidate.contentFingerprint.sha256 ->
                    if (candidate.rule.matchScope == HlsAdMatchScope.GlobalFingerprint) {
                        "全局内容指纹"
                    } else {
                        "内容指纹"
                    }
                candidate.rule.matchScope == HlsAdMatchScope.GlobalFingerprint -> "全局内容指纹"
                else -> "规则匹配"
            }
        )
    }
}
