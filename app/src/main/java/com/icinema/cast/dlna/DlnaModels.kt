package com.icinema.cast.dlna

import com.icinema.cast.CastDevice

internal data class DlnaDevice(
    val id: String,
    val locationUrl: String,
    val friendlyName: String,
    val manufacturer: String,
    val modelName: String,
    val avTransportControlUrl: String
) {
    fun toCastDevice(): CastDevice {
        return CastDevice(
            id = id,
            name = friendlyName,
            manufacturer = manufacturer,
            modelName = modelName
        )
    }
}

internal data class DlnaPlaybackPosition(
    val currentPositionMs: Long,
    val durationMs: Long
)
