package com.icinema.cast.dlna

import java.io.ByteArrayInputStream
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

internal object DlnaDeviceDescriptionParser {
    private const val MEDIA_RENDERER_DEVICE = "urn:schemas-upnp-org:device:MediaRenderer"
    private const val AV_TRANSPORT_SERVICE = "urn:schemas-upnp-org:service:AVTransport"

    fun parse(locationUrl: String, xml: String): DlnaDevice? {
        val document = newDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

        val devices = document.getElementsByTagName("device")
        val mediaRenderer = (0 until devices.length)
            .mapNotNull { devices.item(it) as? Element }
            .firstOrNull { it.childText("deviceType")?.contains(MEDIA_RENDERER_DEVICE) == true }
            ?: return null

        val avTransport = mediaRenderer
            .getElementsByTagName("service")
            .asElementSequence()
            .firstOrNull { service ->
                service.childText("serviceType")?.contains(AV_TRANSPORT_SERVICE) == true &&
                    !service.childText("controlURL").isNullOrBlank()
            }
            ?: return null

        val friendlyName = mediaRenderer.childText("friendlyName").orEmpty().ifBlank { "DLNA 设备" }
        val manufacturer = mediaRenderer.childText("manufacturer").orEmpty()
        val modelName = mediaRenderer.childText("modelName").orEmpty()
        val udn = mediaRenderer.childText("UDN").orEmpty()
        val controlUrl = resolveUrl(locationUrl, avTransport.childText("controlURL").orEmpty())

        return DlnaDevice(
            id = udn.ifBlank { locationUrl },
            locationUrl = locationUrl,
            friendlyName = friendlyName,
            manufacturer = manufacturer,
            modelName = modelName,
            avTransportControlUrl = controlUrl
        )
    }

    private fun newDocumentBuilderFactory(): DocumentBuilderFactory {
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            trySetFeature("http://javax.xml.XMLConstants/feature/secure-processing", true)
            trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            trySetFeature("http://xml.org/sax/features/external-general-entities", false)
            trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isExpandEntityReferences = false
        }
    }

    private fun DocumentBuilderFactory.trySetFeature(name: String, enabled: Boolean) {
        runCatching { setFeature(name, enabled) }
    }

    private fun Element.childText(tagName: String): String? {
        return childNodes.asElementSequence()
            .firstOrNull { it.tagName == tagName }
            ?.textContent
            ?.trim()
    }

    private fun org.w3c.dom.NodeList.asElementSequence(): Sequence<Element> {
        return (0 until length).asSequence().mapNotNull { item(it) as? Element }
    }

    private fun resolveUrl(baseUrl: String, value: String): String {
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        return URI(baseUrl).resolve(value).toString()
    }
}
