package com.icinema.pages.detail

import com.icinema.cast.CastState
import com.icinema.domain.model.Video

object DetailContract {
    data class UiState(
        val currentVideoId: Long? = null,
        val isLoading: Boolean = false,
        val video: Video? = null,
        val error: String? = null,
        val selectedPlaySource: String? = null,
        val selectedEpisode: Int = 0,
        val selectedRange: Int = 0,
        val isFavorite: Boolean = false,
        val hasPlaybackHistory: Boolean = false,
        val restoredByFallback: Boolean = false,
        val castState: CastState = CastState(),
        val isCastSheetVisible: Boolean = false,
        val pendingCastSourceKey: String? = null,
        val pendingCastEpisodeIndex: Int? = null
    )

    sealed interface UiIntent {
        data class LoadVideo(val videoId: Long) : UiIntent
        data object RetryLoad : UiIntent
        data class SelectPlaySource(val source: String) : UiIntent
        data class SelectRange(val range: Int) : UiIntent
        data class SelectEpisode(val episode: Int) : UiIntent
        data class OpenCastFlow(
            val sourceKey: String,
            val episodeIndex: Int
        ) : UiIntent

        data object DismissCastFlow : UiIntent
        data object RefreshCastDevices : UiIntent
        data class SelectCastDevice(val deviceId: String) : UiIntent
        data object ToggleCastPlayPause : UiIntent
        data object StopCasting : UiIntent
        data object ToggleFavorite : UiIntent
        data object ClearVideo : UiIntent
    }

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    sealed interface Mutation {
        data class LoadStarted(val videoId: Long) : Mutation
        data class LoadSucceeded(
            val videoId: Long,
            val video: Video,
            val preferredSource: String?,
            val preferredEpisode: Int,
            val preferredRange: Int,
            val isFavorite: Boolean,
            val hasPlaybackHistory: Boolean,
            val restoredByFallback: Boolean
        ) : Mutation

        data class LoadFailed(
            val videoId: Long,
            val message: String
        ) : Mutation

        data class PlaySourceChanged(val source: String) : Mutation
        data class RangeChanged(val range: Int) : Mutation
        data class EpisodeChanged(val episode: Int) : Mutation
        data class CastSheetChanged(
            val visible: Boolean,
            val sourceKey: String? = null,
            val episodeIndex: Int? = null
        ) : Mutation

        data class CastStateChanged(val castState: CastState) : Mutation
        data class FavoriteChanged(val isFavorite: Boolean) : Mutation
        data object VideoCleared : Mutation
    }
}
