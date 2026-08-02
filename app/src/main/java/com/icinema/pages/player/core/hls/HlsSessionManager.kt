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

    fun prefetch(originUrl: String) {
        prefetchCoordinator.prefetchManifest(originUrl)
    }
}
