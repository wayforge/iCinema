package com.icinema.pages.player.core.hls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsManifestRewriterTest {
    private val rewriter = HlsManifestRewriter()

    @Test
    fun `rewrites master playlist variant urls as manifests`() {
        val playlist = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1280000
            low/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2560000
            https://cdn.example.com/high/index.m3u8
        """.trimIndent()

        val result = rewriter.rewrite(
            playlistUrl = "https://origin.example.com/live/master.m3u8",
            playlist = playlist
        ) { url, type -> "$type:$url" }

        assertTrue(result.playlist.contains("Manifest:https://origin.example.com/live/low/index.m3u8"))
        assertTrue(result.playlist.contains("Manifest:https://cdn.example.com/high/index.m3u8"))
        assertEquals(emptyList<String>(), result.prefetchUrls)
    }

    @Test
    fun `rewrites media playlist resources and collects prefetch urls`() {
        val playlist = """
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="keys/file.key"
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:6.0,
            seg-1.ts
            #EXTINF:6.0,
            https://media.example.com/seg-2.ts
        """.trimIndent()

        val result = rewriter.rewrite(
            playlistUrl = "https://origin.example.com/path/index.m3u8",
            playlist = playlist
        ) { url, type -> "$type:$url" }

        assertTrue(result.playlist.contains("URI=\"Resource:https://origin.example.com/path/keys/file.key\""))
        assertTrue(result.playlist.contains("URI=\"Resource:https://origin.example.com/path/init.mp4\""))
        assertTrue(result.playlist.contains("Resource:https://origin.example.com/path/seg-1.ts"))
        assertTrue(result.playlist.contains("Resource:https://media.example.com/seg-2.ts"))
        assertEquals(
            listOf(
                "https://origin.example.com/path/keys/file.key",
                "https://origin.example.com/path/init.mp4",
                "https://origin.example.com/path/seg-1.ts",
                "https://media.example.com/seg-2.ts"
            ),
            result.prefetchUrls
        )
    }

    @Test
    fun `removes cue out ad segments conservatively`() {
        val playlist = """
            #EXTM3U
            #EXTINF:6.0,
            content-1.ts
            #EXT-X-CUE-OUT:12
            #EXTINF:6.0,
            ad-1.ts
            #EXTINF:6.0,
            ad-2.ts
            #EXT-X-CUE-IN
            #EXTINF:6.0,
            content-2.ts
        """.trimIndent()

        val result = rewriter.rewrite(
            playlistUrl = "https://origin.example.com/vod/index.m3u8",
            playlist = playlist
        ) { url, type -> "$type:$url" }

        assertTrue(result.playlist.contains("Resource:https://origin.example.com/vod/content-1.ts"))
        assertTrue(result.playlist.contains("Resource:https://origin.example.com/vod/content-2.ts"))
        assertFalse(result.playlist.contains("ad-1.ts"))
        assertFalse(result.playlist.contains("ad-2.ts"))
        assertEquals(
            listOf(
                "https://origin.example.com/vod/content-1.ts",
                "https://origin.example.com/vod/content-2.ts"
            ),
            result.prefetchUrls
        )
    }

    @Test
    fun `uses cached ad urls to filter repeated ad segments`() {
        val playlist = """
            #EXTM3U
            #EXTINF:6.0,
            ad_break_001.ts
            #EXTINF:6.0,
            content_001.ts
        """.trimIndent()

        val result = rewriter.rewrite(
            playlistUrl = "https://origin.example.com/live/index.m3u8",
            playlist = playlist,
            knownAdResourceUrls = setOf("https://origin.example.com/live/ad_break_001.ts")
        ) { url, type -> "$type:$url" }

        assertFalse(result.playlist.contains("ad_break_001.ts"))
        assertTrue(result.playlist.contains("content_001.ts"))
    }
}
