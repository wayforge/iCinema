package com.icinema.cast.dlna

import org.junit.Assert.assertEquals
import org.junit.Test

class DlnaDiscoveryParserTest {
    @Test
    fun parseLocation_readsCaseInsensitiveLocationHeader() {
        val response = """
            HTTP/1.1 200 OK
            CACHE-CONTROL: max-age=1800
            LOCATION: http://192.168.1.20:49152/rootDesc.xml
            ST: urn:schemas-upnp-org:device:MediaRenderer:1
            USN: uuid:tv::urn:schemas-upnp-org:device:MediaRenderer:1
        """.trimIndent()

        assertEquals(
            "http://192.168.1.20:49152/rootDesc.xml",
            DlnaDiscoveryParser.parseLocation(response)
        )
    }
}
