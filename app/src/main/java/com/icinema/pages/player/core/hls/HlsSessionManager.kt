package com.icinema.pages.player.core.hls

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsSessionManager @Inject constructor(
    private val proxyServer: HlsProxyServer,
    private val prefetchCoordinator: HlsPrefetchCoordinator
) {
    fun playbackUrl(originUrl: String): String {
        return proxyServer.proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Manifest,
            target = HlsProxyTarget.Loopback
        )
    }

    fun castUrl(originUrl: String): String {
        return proxyServer.proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Manifest,
            target = HlsProxyTarget.Lan
        )
    }

    fun resourcePlaybackUrl(originUrl: String): String {
        return proxyServer.resourceUrl(originUrl, HlsProxyTarget.Loopback)
    }

    fun prefetch(originUrl: String) {
        prefetchCoordinator.prefetchManifest(originUrl)
    }

    fun markCurrentSegmentAsAd(
        playbackPositionMs: Long,
        videoTitle: String,
        episodeTitle: String
    ): Result<String> {
        return proxyServer.markCurrentSegmentAsAd(
            playbackPositionMs = playbackPositionMs,
            videoTitle = videoTitle,
            episodeTitle = episodeTitle
        ).onSuccess { segment ->
            prefetchCoordinator.prefetchResources(listOf(segment.rule.segmentUrl))
        }.map { it.message }
    }
}
