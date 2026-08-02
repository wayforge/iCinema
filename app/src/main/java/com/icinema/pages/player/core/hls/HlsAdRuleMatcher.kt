package com.icinema.pages.player.core.hls

import java.net.URI

internal object HlsAdRuleMatcher {
    fun appliesToPlaylist(rule: HlsAdRule, candidatePlaylistUrl: String): Boolean {
        return sameUrl(rule.playlistUrl, candidatePlaylistUrl) ||
            sameUrlIgnoringQuery(rule.playlistUrl, candidatePlaylistUrl)
    }

    fun matches(rule: HlsAdRule, candidateSegmentUrl: String): Boolean {
        if (sameUrl(rule.segmentUrl, candidateSegmentUrl)) return true
        if (sameUrlIgnoringQuery(rule.segmentUrl, candidateSegmentUrl)) return true

        val ruleFileName = fileName(rule.segmentUrl).ifBlank { rule.matchText }
        val candidateFileName = fileName(candidateSegmentUrl)
        if (ruleFileName.isNotBlank() && ruleFileName == candidateFileName) return true

        val pattern = rule.urlPattern ?: return false
        return runCatching { Regex(pattern).matches(candidateSegmentUrl) }.getOrDefault(false)
    }

    private fun sameUrl(first: String, second: String): Boolean {
        return first.trim() == second.trim()
    }

    private fun sameUrlIgnoringQuery(first: String, second: String): Boolean {
        val firstParts = first.uriPartsWithoutQuery()
        val secondParts = second.uriPartsWithoutQuery()
        if (firstParts != null && secondParts != null) {
            return firstParts == secondParts
        }
        return first.substringBefore('?') == second.substringBefore('?')
    }

    private fun String.uriPartsWithoutQuery(): UrlParts? {
        return runCatching {
            val uri = URI(this)
            val scheme = uri.scheme?.lowercase().orEmpty()
            val host = uri.host?.lowercase().orEmpty()
            val port = uri.port
            val path = uri.path.orEmpty()
            if (scheme.isBlank() || host.isBlank() || path.isBlank()) {
                null
            } else {
                UrlParts(scheme, host, port, path)
            }
        }.getOrNull()
    }

    private fun fileName(url: String): String {
        return runCatching {
            URI(url.substringBefore('?')).path.orEmpty().substringAfterLast('/')
        }.getOrDefault(url.substringBefore('?').substringAfterLast('/'))
    }

    private data class UrlParts(
        val scheme: String,
        val host: String,
        val port: Int,
        val path: String
    )
}
