package com.icinema.pages.player.core.hls

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsSessionManager @Inject constructor(
    private val proxyServer: HlsProxyServer,
    private val prefetchCoordinator: HlsPrefetchCoordinator
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
}
