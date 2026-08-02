package com.icinema.pages.player.core.hls

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsAdRuleMatcherTest {
    @Test
    fun `applies to same playlist ignoring query`() {
        val rule = rule(
            playlistUrl = "https://cdn.example.com/show/episode/index.m3u8?token=old",
            segmentUrl = "https://cdn.example.com/show/episode/ad.ts?hash=old"
        )

        assertTrue(
            HlsAdRuleMatcher.appliesToPlaylist(
                rule,
                "https://cdn.example.com/show/episode/index.m3u8?token=new"
            )
        )
    }

    @Test
    fun `does not apply to sibling playlist on same host`() {
        val rule = rule(
            playlistUrl = "https://cdn.example.com/show/episode-1/index.m3u8",
            segmentUrl = "https://cdn.example.com/show/episode-1/ad.ts"
        )

        assertFalse(
            HlsAdRuleMatcher.appliesToPlaylist(
                rule,
                "https://cdn.example.com/show/episode-2/index.m3u8"
            )
        )
    }

    @Test
    fun `matches segment file name ignoring query`() {
        val rule = rule(
            playlistUrl = "https://cdn.example.com/show/episode/index.m3u8",
            segmentUrl = "https://cdn.example.com/show/episode/ab9c0a82.ts?hash=old"
        )

        assertTrue(HlsAdRuleMatcher.matches(rule, "https://cdn.example.com/show/episode/ab9c0a82.ts?hash=new"))
    }

    private fun rule(
        playlistUrl: String,
        segmentUrl: String
    ): HlsAdRule {
        return HlsAdRule(
            id = "rule",
            playlistUrl = playlistUrl,
            segmentUrl = segmentUrl,
            urlPattern = null,
            matchText = segmentUrl.substringBefore('?').substringAfterLast('/'),
            durationSeconds = 3.0,
            videoTitle = "video",
            episodeTitle = "episode",
            createdAtMs = 1L,
            updatedAtMs = 1L
        )
    }
}
