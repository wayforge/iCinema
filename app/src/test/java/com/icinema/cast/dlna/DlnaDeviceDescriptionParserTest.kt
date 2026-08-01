package com.icinema.cast.dlna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DlnaDeviceDescriptionParserTest {
    @Test
    fun parse_returnsMediaRendererWithResolvedAvTransportControlUrl() {
        val xml = """
            <?xml version="1.0"?>
            <root>
                <device>
                    <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
                    <friendlyName>小米电视</friendlyName>
                    <manufacturer>Xiaomi</manufacturer>
                    <modelName>MiTV</modelName>
                    <UDN>uuid:xiaomi-tv</UDN>
                    <serviceList>
                        <service>
                            <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                            <controlURL>/upnp/control/AVTransport</controlURL>
                        </service>
                    </serviceList>
                </device>
            </root>
        """.trimIndent()

        val device = DlnaDeviceDescriptionParser.parse(
            locationUrl = "http://192.168.1.20:49152/rootDesc.xml",
            xml = xml
        )

        assertNotNull(device)
        assertEquals("uuid:xiaomi-tv", device?.id)
        assertEquals("小米电视", device?.friendlyName)
        assertEquals("Xiaomi", device?.manufacturer)
        assertEquals("MiTV", device?.modelName)
        assertEquals("http://192.168.1.20:49152/upnp/control/AVTransport", device?.avTransportControlUrl)
    }
}
