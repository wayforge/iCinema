package com.icinema.cast.dlna

internal object DlnaDiscoveryParser {
    fun parseLocation(response: String): String? {
        return parseHeaders(response)["location"]?.takeIf { it.isNotBlank() }
    }

    fun parseHeaders(response: String): Map<String, String> {
        return response
            .lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) {
                    null
                } else {
                    line.substring(0, separator).trim().lowercase() to
                        line.substring(separator + 1).trim()
                }
            }
            .toMap()
    }
}
