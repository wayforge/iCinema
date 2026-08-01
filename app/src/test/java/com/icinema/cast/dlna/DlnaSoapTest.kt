package com.icinema.cast.dlna

import com.icinema.cast.CastMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DlnaSoapTest {
    @Test
    fun seekArguments_formatsMillisecondsAsDlnaRelTime() {
        assertEquals("01:02:03", DlnaSoap.formatDlnaTime(3_723_000L))
        assertEquals(3_723_000L, DlnaSoap.parseDlnaTime("01:02:03"))
    }

    @Test
    fun setUriArguments_escapesUrlAndMetadata() {
        val arguments = DlnaSoap.setUriArguments(
            CastMedia(
                url = "https://example.com/movie.m3u8?token=a&b=1",
                title = "电影 <第一集>",
                imageUrl = "https://example.com/poster.jpg"
            )
        )

        assertTrue(arguments.contains("https://example.com/movie.m3u8?token=a&amp;b=1"))
        assertTrue(arguments.contains("电影 &amp;lt;第一集&amp;gt;"))
        assertTrue(arguments.contains("CurrentURIMetaData"))
    }

    @Test
    fun parsePositionInfo_readsRelTimeAndDuration() {
        val xml = """
            <s:Envelope>
                <s:Body>
                    <u:GetPositionInfoResponse>
                        <TrackDuration>01:30:00</TrackDuration>
                        <RelTime>00:10:05</RelTime>
                    </u:GetPositionInfoResponse>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val position = DlnaSoap.parsePositionInfo(xml)

        assertEquals(605_000L, position?.currentPositionMs)
        assertEquals(5_400_000L, position?.durationMs)
    }
}
