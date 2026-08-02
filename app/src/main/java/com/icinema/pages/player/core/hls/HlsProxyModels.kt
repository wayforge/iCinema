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
