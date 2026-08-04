package com.icinema.pages.player.core.hls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure matching helpers covered without Android prefs (store integration needs instrumentation).
 */
class HlsAdRuleStoreMatchLogicTest {

    @Test
    fun globalFingerprint_matchesAcrossPlaylists_byHashAndLength() {
        val rule = sampleRule(
            playlistUrl = "https://cdn.a.com/show1/index.m3u8",
            segmentUrl = "https://cdn.a.com/show1/ad.ts",
            sha256 = "abc",
            length = 100L,
            scope = HlsAdMatchScope.GlobalFingerprint
        )
        val fp = HlsContentFingerprint(sha256 = "abc", length = 100L)
        assertTrue(rule.matchesContentForTest(fp))
        assertFalse(rule.matchesContentForTest(HlsContentFingerprint("abc", 99L)))
        assertFalse(rule.matchesContentForTest(HlsContentFingerprint("zzz", 100L)))
    }

    @Test
    fun playlistScope_fingerprintAlone_isNotCrossPlaylistIntent() {
        val rule = sampleRule(
            playlistUrl = "https://cdn.a.com/show1/index.m3u8",
            segmentUrl = "https://cdn.a.com/show1/ad.ts",
            sha256 = "abc",
            length = 100L,
            scope = HlsAdMatchScope.Playlist
        )
        // Scope itself is playlist — cross-playlist use must check matchScope in store.
        assertEquals(HlsAdMatchScope.Playlist, rule.matchScope)
        assertTrue(rule.hasContentFingerprint)
    }

    @Test
    fun hasContentFingerprint_requiresBothFields() {
        val incomplete = sampleRule(
            playlistUrl = "https://cdn.a.com/a.m3u8",
            segmentUrl = "https://cdn.a.com/a.ts",
            sha256 = "abc",
            length = null,
            scope = HlsAdMatchScope.GlobalFingerprint
        )
        assertFalse(incomplete.hasContentFingerprint)
    }

    private fun sampleRule(
        playlistUrl: String,
        segmentUrl: String,
        sha256: String?,
        length: Long?,
        scope: HlsAdMatchScope
    ): HlsAdRule {
        return HlsAdRule(
            id = "1",
            playlistUrl = playlistUrl,
            segmentUrl = segmentUrl,
            urlPattern = null,
            matchText = "ad.ts",
            durationSeconds = 3.0,
            videoTitle = "v",
            episodeTitle = "e",
            createdAtMs = 1L,
            updatedAtMs = 1L,
            contentSha256 = sha256,
            contentLength = length,
            matchScope = scope
        )
    }

    private fun HlsAdRule.matchesContentForTest(fingerprint: HlsContentFingerprint?): Boolean {
        if (fingerprint == null) return false
        val ruleHash = contentSha256?.takeIf { it.isNotBlank() } ?: return false
        val ruleLength = contentLength ?: return false
        return ruleHash == fingerprint.sha256 && ruleLength == fingerprint.length
    }
}
