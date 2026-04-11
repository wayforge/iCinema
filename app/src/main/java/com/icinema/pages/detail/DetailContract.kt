package com.icinema.pages.detail

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
        val restoredByFallback: Boolean = false
    )

    sealed interface UiIntent {
        data class LoadVideo(val videoId: Long) : UiIntent
        data object RetryLoad : UiIntent
        data class SelectPlaySource(val source: String) : UiIntent
        data class SelectRange(val range: Int) : UiIntent
        data class SelectEpisode(val episode: Int) : UiIntent
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
        data class FavoriteChanged(val isFavorite: Boolean) : Mutation
        data object VideoCleared : Mutation
    }
}
