package com.icinema.pages.player

import javax.inject.Inject

class PlayerReducer @Inject constructor() {
    fun reduce(
        current: PlayerContract.UiState,
        mutation: PlayerContract.Mutation
    ): PlayerContract.UiState {
        return when (mutation) {
            is PlayerContract.Mutation.LoadStarted -> {
                current.copy(
                    videoId = mutation.videoId,
                    isLoading = true,
                    error = null,
                    selectedSourceKey = mutation.sourceKey,
                    selectedEpisodeIndex = mutation.episodeIndex,
                    currentPositionMs = 0,
                    durationMs = 0,
                    bufferedPositionMs = 0,
                    resumePositionMs = null
                )
            }

            is PlayerContract.Mutation.LoadSucceeded -> {
                current.copy(
                    videoId = mutation.videoId,
                    video = mutation.video,
                    playSources = mutation.playSources,
                    selectedSourceKey = mutation.sourceKey,
                    selectedEpisodeIndex = mutation.episodeIndex,
                    currentEpisode = mutation.currentEpisode,
                    isLoading = false,
                    error = null,
                    canPlayNext = mutation.episodeIndex < mutation.playSources
                        .firstOrNull { it.key == mutation.sourceKey }
                        ?.episodes
                        ?.lastIndex
                        .orMinusOne(),
                    resumePositionMs = mutation.resumePositionMs
                )
            }

            is PlayerContract.Mutation.LoadFailed -> {
                current.copy(
                    isLoading = false,
                    error = mutation.message,
                    isPlaying = false,
                    isBuffering = false
                )
            }

            is PlayerContract.Mutation.SourceSelected -> {
                current.copy(
                    selectedSourceKey = mutation.sourceKey,
                    selectedEpisodeIndex = mutation.episodeIndex,
                    currentEpisode = mutation.currentEpisode,
                    canPlayNext = mutation.canPlayNext,
                    error = null,
                    currentPositionMs = 0,
                    durationMs = 0,
                    bufferedPositionMs = 0,
                    resumePositionMs = null,
                    activeSheetMode = null
                )
            }

            is PlayerContract.Mutation.EpisodeSelected -> {
                current.copy(
                    selectedEpisodeIndex = mutation.episodeIndex,
                    currentEpisode = mutation.currentEpisode,
                    canPlayNext = mutation.canPlayNext,
                    error = null,
                    currentPositionMs = 0,
                    durationMs = 0,
                    bufferedPositionMs = 0,
                    resumePositionMs = null,
                    activeSheetMode = null
                )
            }

            is PlayerContract.Mutation.PlaybackChanged -> {
                current.copy(
                    isPlaying = mutation.isPlaying,
                    isBuffering = mutation.isBuffering
                )
            }

            is PlayerContract.Mutation.PositionChanged -> {
                current.copy(
                    currentPositionMs = mutation.currentPositionMs,
                    durationMs = mutation.durationMs,
                    bufferedPositionMs = mutation.bufferedPositionMs
                )
            }

            is PlayerContract.Mutation.ControlsVisibilityChanged -> {
                current.copy(controlsVisible = mutation.visible)
            }

            is PlayerContract.Mutation.SheetModeChanged -> {
                current.copy(activeSheetMode = mutation.mode)
            }

            is PlayerContract.Mutation.FullscreenChanged -> {
                current.copy(isFullscreen = mutation.isFullscreen)
            }

            is PlayerContract.Mutation.ErrorChanged -> {
                current.copy(error = mutation.message)
            }

            is PlayerContract.Mutation.ResumePositionChanged -> {
                current.copy(resumePositionMs = mutation.positionMs)
            }
        }
    }

    private fun Int?.orMinusOne(): Int = this ?: -1
}
