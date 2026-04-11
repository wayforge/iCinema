package com.icinema.pages.detail

import javax.inject.Inject

class DetailReducer @Inject constructor() {
    fun reduce(
        current: DetailContract.UiState,
        mutation: DetailContract.Mutation
    ): DetailContract.UiState {
        return when (mutation) {
            is DetailContract.Mutation.LoadStarted -> {
                current.copy(
                    currentVideoId = mutation.videoId,
                    isLoading = true,
                    video = null,
                    error = null,
                    selectedPlaySource = null,
                    selectedEpisode = 0,
                    selectedRange = 0,
                    hasPlaybackHistory = false,
                    restoredByFallback = false
                )
            }

            is DetailContract.Mutation.LoadSucceeded -> {
                current.copy(
                    currentVideoId = mutation.videoId,
                    isLoading = false,
                    video = mutation.video,
                    error = null,
                    selectedPlaySource = mutation.preferredSource,
                    selectedEpisode = mutation.preferredEpisode,
                    selectedRange = mutation.preferredRange,
                    isFavorite = mutation.isFavorite,
                    hasPlaybackHistory = mutation.hasPlaybackHistory,
                    restoredByFallback = mutation.restoredByFallback
                )
            }

            is DetailContract.Mutation.LoadFailed -> {
                current.copy(
                    currentVideoId = mutation.videoId,
                    isLoading = false,
                    video = null,
                    error = mutation.message,
                    selectedPlaySource = null,
                    selectedEpisode = 0,
                    selectedRange = 0
                )
            }

            is DetailContract.Mutation.PlaySourceChanged -> {
                current.copy(
                    selectedPlaySource = mutation.source,
                    selectedEpisode = 0,
                    selectedRange = 0
                )
            }

            is DetailContract.Mutation.RangeChanged -> {
                current.copy(selectedRange = mutation.range)
            }

            is DetailContract.Mutation.EpisodeChanged -> {
                current.copy(selectedEpisode = mutation.episode)
            }

            is DetailContract.Mutation.FavoriteChanged -> {
                current.copy(isFavorite = mutation.isFavorite)
            }

            DetailContract.Mutation.VideoCleared -> {
                DetailContract.UiState()
            }
        }
    }
}
