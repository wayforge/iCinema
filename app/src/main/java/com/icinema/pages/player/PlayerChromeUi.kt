package com.icinema.pages.player

import com.icinema.cast.CastState
import com.icinema.domain.model.PlayableEpisode
import com.icinema.domain.model.Video

/**
 * Structural player chrome snapshot without high-frequency clock fields.
 * Equality skips position ticks so top/transport bars are not recomposed every 250ms.
 */
data class PlayerChromeUi(
    val video: Video? = null,
    val currentEpisode: PlayableEpisode? = null,
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val controlsVisible: Boolean = true,
    val error: String? = null,
    val errorDetail: String? = null,
    val resumePositionMs: Long? = null,
    val playbackSpeed: Float = 1.0f,
    val autoPlayNextEnabled: Boolean = true,
    val gestureSeekEnabled: Boolean = true,
    val castState: CastState = CastState(),
    val playerToast: String? = null,
    val playerToastToken: Long = 0L
)

fun PlayerContract.UiState.toChromeUi(): PlayerChromeUi {
    return PlayerChromeUi(
        video = video,
        currentEpisode = currentEpisode,
        isLoading = isLoading,
        isBuffering = isBuffering,
        isPlaying = isPlaying,
        controlsVisible = controlsVisible,
        error = error,
        errorDetail = errorDetail,
        resumePositionMs = resumePositionMs,
        playbackSpeed = playbackSpeed,
        autoPlayNextEnabled = autoPlayNextEnabled,
        gestureSeekEnabled = gestureSeekEnabled,
        castState = castState,
        playerToast = playerToast,
        playerToastToken = playerToastToken
    )
}
