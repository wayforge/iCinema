package com.icinema.pages.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodePickerSupportTest {

    @Test
    fun rangeSize_scalesWithTotal() {
        assertEquals(1, episodeRangeSize(0))
        assertEquals(12, episodeRangeSize(12))
        assertEquals(24, episodeRangeSize(24))
        assertEquals(20, episodeRangeSize(25))
        assertEquals(20, episodeRangeSize(100))
        assertEquals(50, episodeRangeSize(101))
        assertEquals(50, episodeRangeSize(500))
        assertEquals(100, episodeRangeSize(501))
        assertEquals(100, episodeRangeSize(1000))
    }

    @Test
    fun rangeCount_forThousandEpisodes() {
        assertEquals(10, episodeRangeCount(1000))
        assertEquals(1, episodeRangeCount(18))
        assertEquals(2, episodeRangeCount(30))
    }

    @Test
    fun rangeIndex_locatesCurrentEpisode() {
        assertEquals(0, episodeRangeIndex(0, 1000))
        assertEquals(8, episodeRangeIndex(836, 1000)) // 801-900
        assertEquals(9, episodeRangeIndex(999, 1000))
        assertEquals(1, episodeRangeIndex(20, 30)) // size 20 → 21-30
    }

    @Test
    fun rangeBounds_areHalfOpenByIndex() {
        val bounds = episodeRangeBounds(8, 1000)
        assertEquals(800, bounds.first)
        assertEquals(899, bounds.last)
    }

    @Test
    fun denseLayout_whenMostTitlesAreNumeric() {
        val titles = List(30) { "第${it + 1}集" }
        assertTrue(isDenseEpisodeLayout(titles))
        assertTrue(isShortEpisodeTitle("12"))
        assertTrue(isShortEpisodeTitle("EP12"))
        assertTrue(isShortEpisodeTitle("E-3"))
        assertFalse(isShortEpisodeTitle("HD中字完整版"))
        assertEquals("12", denseEpisodeLabel("第12集", 11))
    }

    @Test
    fun infoLayout_whenTitlesAreNames() {
        val titles = listOf("枪版", "HD中字", "导演剪辑版", "预告片")
        assertFalse(isDenseEpisodeLayout(titles))
    }
}
