package com.icinema.pages.player

import com.icinema.domain.model.PlaySource
import com.icinema.domain.model.PlayableEpisode
import com.icinema.domain.model.Video

private const val DEFAULT_PLAYBACK_SPEED = 1.0f

object PlayerContract {
    enum class SheetMode {
        Sources,
        Episodes,
        Details
    }

    data class UiState(
        val videoId: Long? = null,
        val video: Video? = null,
        val playSources: List<PlaySource> = emptyList(),
        val selectedSourceKey: String? = null,
        val selectedEpisodeIndex: Int = 0,
        val currentEpisode: PlayableEpisode? = null,
        val isLoading: Boolean = false,
        val isBuffering: Boolean = false,
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0,
        val durationMs: Long = 0,
        val bufferedPositionMs: Long = 0,
        val controlsVisible: Boolean = true,
        val error: String? = null,
        val canPlayNext: Boolean = false,
        val cacheEnabled: Boolean = true,
        val isFullscreen: Boolean = true,
        val resumePositionMs: Long? = null,
        val activeSheetMode: SheetMode? = null,
        val playbackSpeed: Float = DEFAULT_PLAYBACK_SPEED,
        val autoPlayNextEnabled: Boolean = true,
        val controlsLocked: Boolean = false,
        val gestureSeekEnabled: Boolean = true
    )

    sealed interface UiIntent {
        data class Load(
            val videoId: Long,
            val sourceKey: String?,
            val episodeIndex: Int
        ) : UiIntent

        data object TogglePlayPause : UiIntent
        data class SeekTo(val positionMs: Long) : UiIntent
        data object SeekForward : UiIntent
        data object SeekBackward : UiIntent
        data class SelectSource(val sourceKey: String) : UiIntent
        data class SelectEpisode(val episodeIndex: Int) : UiIntent
        data object PlayNext : UiIntent
        data object PlayPrevious : UiIntent
        data object Retry : UiIntent
        data object ToggleControls : UiIntent
        data class OpenSheet(val mode: SheetMode) : UiIntent
        data object DismissSheet : UiIntent
        data object EnterFullscreen : UiIntent
        data object ExitFullscreen : UiIntent
        data object AcceptResume : UiIntent
        data object RestartFromBeginning : UiIntent
        data class SetPlaybackSpeed(val speed: Float) : UiIntent
        data object ToggleAutoPlayNext : UiIntent
        data object ToggleControlsLock : UiIntent
        data object ToggleGestureSeek : UiIntent
        data class GestureSeek(val deltaMs: Long) : UiIntent
        data object OnLifecycleStart : UiIntent
        data object OnLifecycleStop : UiIntent
    }

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    sealed interface Mutation {
        data class LoadStarted(
            val videoId: Long,
            val sourceKey: String?,
            val episodeIndex: Int
        ) : Mutation

        data class LoadSucceeded(
            val videoId: Long,
            val video: Video,
            val playSources: List<PlaySource>,
            val sourceKey: String,
            val episodeIndex: Int,
            val currentEpisode: PlayableEpisode,
            val resumePositionMs: Long?
        ) : Mutation

        data class LoadFailed(val message: String) : Mutation
        data class SourceSelected(
            val sourceKey: String,
            val episodeIndex: Int,
            val currentEpisode: PlayableEpisode,
            val canPlayNext: Boolean
        ) : Mutation

        data class EpisodeSelected(
            val episodeIndex: Int,
            val currentEpisode: PlayableEpisode,
            val canPlayNext: Boolean
        ) : Mutation

        data class PlaybackChanged(
            val isPlaying: Boolean,
            val isBuffering: Boolean
        ) : Mutation

        data class PositionChanged(
            val currentPositionMs: Long,
            val durationMs: Long,
            val bufferedPositionMs: Long
        ) : Mutation

        data class ControlsVisibilityChanged(val visible: Boolean) : Mutation
        data class SheetModeChanged(val mode: SheetMode?) : Mutation
        data class FullscreenChanged(val isFullscreen: Boolean) : Mutation
        data class ErrorChanged(val message: String?) : Mutation
        data class ResumePositionChanged(val positionMs: Long?) : Mutation
        data class SettingsLoaded(
            val playbackSpeed: Float,
            val autoPlayNextEnabled: Boolean,
            val gestureSeekEnabled: Boolean
        ) : Mutation
        data class PlaybackSpeedChanged(val speed: Float) : Mutation
        data class AutoPlayNextChanged(val enabled: Boolean) : Mutation
        data class ControlsLockedChanged(val locked: Boolean) : Mutation
        data class GestureSeekChanged(val enabled: Boolean) : Mutation
    }
}
