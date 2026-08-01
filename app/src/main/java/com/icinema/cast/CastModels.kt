package com.icinema.cast

import kotlinx.coroutines.flow.StateFlow

data class CastDevice(
    val id: String,
    val name: String,
    val manufacturer: String = "",
    val modelName: String = ""
)

data class CastMedia(
    val url: String,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val contentType: String = HLS_CONTENT_TYPE
) {
    companion object {
        const val HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl"
    }
}

data class CastState(
    val isSearching: Boolean = false,
    val isConnecting: Boolean = false,
    val devices: List<CastDevice> = emptyList(),
    val connectedDevice: CastDevice? = null,
    val isCasting: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

interface CastController {
    val state: StateFlow<CastState>

    suspend fun startDiscovery(timeoutMs: Long = 8_000L)

    suspend fun cast(deviceId: String, media: CastMedia): Result<Unit>

    suspend fun play(): Result<Unit>

    suspend fun pause(): Result<Unit>

    suspend fun seekTo(positionMs: Long): Result<Unit>

    suspend fun refreshPlaybackPosition(): Result<Unit>

    suspend fun stopCasting(): Result<Long?>

    fun release()
}
