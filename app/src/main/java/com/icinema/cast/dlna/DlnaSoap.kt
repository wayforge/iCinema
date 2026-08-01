package com.icinema.cast.dlna

import com.icinema.cast.CastMedia

internal object DlnaSoap {
    const val AV_TRANSPORT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"

    fun envelope(action: String, arguments: String): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:$action xmlns:u="$AV_TRANSPORT_SERVICE">
                        $arguments
                    </u:$action>
                </s:Body>
            </s:Envelope>
        """.trimIndent()
    }

    fun setUriArguments(media: CastMedia): String {
        val metadata = didlLiteMetadata(media)
        return """
            <InstanceID>0</InstanceID>
            <CurrentURI>${media.url.escapeXml()}</CurrentURI>
            <CurrentURIMetaData>${metadata.escapeXml()}</CurrentURIMetaData>
        """.trimIndent()
    }

    fun playArguments(): String {
        return """
            <InstanceID>0</InstanceID>
            <Speed>1</Speed>
        """.trimIndent()
    }

    fun instanceArguments(): String {
        return "<InstanceID>0</InstanceID>"
    }

    fun seekArguments(positionMs: Long): String {
        return """
            <InstanceID>0</InstanceID>
            <Unit>REL_TIME</Unit>
            <Target>${formatDlnaTime(positionMs)}</Target>
        """.trimIndent()
    }

    fun didlLiteMetadata(media: CastMedia): String {
        val art = media.imageUrl.takeIf { it.isNotBlank() }?.let { imageUrl ->
            "<upnp:albumArtURI>${imageUrl.escapeXml()}</upnp:albumArtURI>"
        }.orEmpty()

        return """
            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
                <item id="0" parentID="0" restricted="1">
                    <dc:title>${media.title.escapeXml()}</dc:title>
                    <upnp:class>object.item.videoItem</upnp:class>
                    $art
                    <res protocolInfo="http-get:*:${media.contentType.escapeXml()}:*">${media.url.escapeXml()}</res>
                </item>
            </DIDL-Lite>
        """.trimIndent()
    }

    fun parsePositionInfo(xml: String): DlnaPlaybackPosition? {
        val relTime = xml.extractTagValue("RelTime") ?: return null
        val duration = xml.extractTagValue("TrackDuration").orEmpty()
        return DlnaPlaybackPosition(
            currentPositionMs = parseDlnaTime(relTime),
            durationMs = parseDlnaTime(duration)
        )
    }

    fun formatDlnaTime(positionMs: Long): String {
        val totalSeconds = (positionMs / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun parseDlnaTime(value: String): Long {
        val parts = value.substringBefore('.').split(':')
        if (parts.size != 3) return 0L
        val hours = parts[0].toLongOrNull() ?: return 0L
        val minutes = parts[1].toLongOrNull() ?: return 0L
        val seconds = parts[2].toLongOrNull() ?: return 0L
        return ((hours * 3_600L) + (minutes * 60L) + seconds) * 1_000L
    }

    private fun String.extractTagValue(tagName: String): String? {
        val regex = Regex("<(?:\\w+:)?$tagName>(.*?)</(?:\\w+:)?$tagName>", RegexOption.DOT_MATCHES_ALL)
        return regex.find(this)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun String.escapeXml(): String {
        return buildString(length) {
            this@escapeXml.forEach { char ->
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(char)
                }
            }
        }
    }
}
