package com.icinema.pages.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val bizPort: DetailBizPort,
    private val reducer: DetailReducer
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailContract.UiState())
    val uiState: StateFlow<DetailContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<DetailContract.UiEffect>()
    val uiEffect: Flow<DetailContract.UiEffect> = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: DetailContract.UiIntent) {
        when (intent) {
            is DetailContract.UiIntent.LoadVideo -> loadVideo(intent.videoId)
            DetailContract.UiIntent.RetryLoad -> retryLoad()
            is DetailContract.UiIntent.SelectPlaySource -> selectPlaySource(intent.source)
            is DetailContract.UiIntent.SelectRange -> selectRange(intent.range)
            is DetailContract.UiIntent.SelectEpisode -> selectEpisode(intent.episode)
            DetailContract.UiIntent.ToggleFavorite -> toggleFavorite()
            DetailContract.UiIntent.ClearVideo -> clearVideo()
        }
    }

    private fun loadVideo(videoId: Long) {
        viewModelScope.launch {
            commit(DetailContract.Mutation.LoadStarted(videoId))
            bizPort.loadVideo(videoId)
                .onSuccess { video ->
                    val isFavorite = bizPort.isFavorite(videoId).getOrDefault(false)
                    val latestPlayback = bizPort.loadLatestPlayback(videoId).getOrNull()
                    val selection = resolvePreferredSelection(video, latestPlayback)
                    commit(
                        DetailContract.Mutation.LoadSucceeded(
                            videoId = videoId,
                            video = video,
                            preferredSource = selection.sourceKey,
                            preferredEpisode = selection.episodeIndex,
                            preferredRange = selection.rangeIndex,
                            isFavorite = isFavorite,
                            hasPlaybackHistory = latestPlayback != null,
                            restoredByFallback = selection.restoredByFallback
                        )
                    )
                }
                .onFailure { exception ->
                    val message = exception.message ?: "详情加载失败"
                    commit(DetailContract.Mutation.LoadFailed(videoId, message))
                    emitEffect(DetailContract.UiEffect.ShowMessage(message))
                }
        }
    }

    private fun retryLoad() {
        val videoId = _uiState.value.currentVideoId
        if (videoId == null) {
            emitEffect(DetailContract.UiEffect.ShowMessage("缺少视频标识，无法重试"))
            return
        }
        handleIntent(DetailContract.UiIntent.LoadVideo(videoId))
    }

    private fun selectPlaySource(source: String) {
        if (_uiState.value.selectedPlaySource != source) {
            commit(DetailContract.Mutation.PlaySourceChanged(source))
        }
    }

    private fun selectRange(range: Int) {
        if (_uiState.value.selectedRange != range) {
            commit(DetailContract.Mutation.RangeChanged(range))
        }
    }

    private fun selectEpisode(episode: Int) {
        if (_uiState.value.selectedEpisode != episode) {
            commit(DetailContract.Mutation.EpisodeChanged(episode))
        }
    }

    private fun toggleFavorite() {
        val video = _uiState.value.video ?: return
        viewModelScope.launch {
            bizPort.toggleFavorite(video)
                .onSuccess { isFavorite ->
                    commit(DetailContract.Mutation.FavoriteChanged(isFavorite))
                    emitEffect(
                        DetailContract.UiEffect.ShowMessage(
                            if (isFavorite) "已加入收藏" else "已取消收藏"
                        )
                    )
                }
                .onFailure {
                    emitEffect(DetailContract.UiEffect.ShowMessage(it.message ?: "收藏操作失败"))
                }
        }
    }

    private fun clearVideo() {
        commit(DetailContract.Mutation.VideoCleared)
    }

    private data class PreferredSelection(
        val sourceKey: String?,
        val episodeIndex: Int,
        val rangeIndex: Int,
        val restoredByFallback: Boolean
    )

    private fun resolvePreferredSelection(
        video: com.icinema.domain.model.Video,
        latestPlayback: com.icinema.domain.model.WatchHistoryItem?
    ): PreferredSelection {
        val playSources = video.playSources
        if (playSources.isEmpty()) {
            return PreferredSelection(sourceKey = null, episodeIndex = 0, rangeIndex = 0, restoredByFallback = false)
        }

        val defaultSource = playSources.firstOrNull { source ->
            source.episodes.any { it.isHls }
        } ?: playSources.first()
        val defaultEpisode = defaultSource.episodes.firstOrNull { it.isHls } ?: defaultSource.episodes.first()

        if (latestPlayback == null) {
            return PreferredSelection(
                sourceKey = defaultSource.key,
                episodeIndex = defaultEpisode.index,
                rangeIndex = calculateRangeIndex(defaultSource.episodes.size, defaultEpisode.index),
                restoredByFallback = false
            )
        }

        val matchedSource = playSources.firstOrNull { it.key == latestPlayback.sourceKey }
            ?: return PreferredSelection(
                sourceKey = defaultSource.key,
                episodeIndex = defaultEpisode.index,
                rangeIndex = calculateRangeIndex(defaultSource.episodes.size, defaultEpisode.index),
                restoredByFallback = true
            )

        val targetEpisode = matchedSource.episodes.firstOrNull { it.index == latestPlayback.episodeIndex }

        if (targetEpisode == null) {
            val fallbackEpisode = matchedSource.episodes.firstOrNull { it.isHls } ?: matchedSource.episodes.first()
            return PreferredSelection(
                sourceKey = matchedSource.key,
                episodeIndex = fallbackEpisode.index,
                rangeIndex = calculateRangeIndex(matchedSource.episodes.size, fallbackEpisode.index),
                restoredByFallback = true
            )
        }

        if (latestPlayback.completed) {
            val nextEpisode = matchedSource.episodes.getOrNull(targetEpisode.index + 1)
            if (nextEpisode != null) {
                return PreferredSelection(
                    sourceKey = matchedSource.key,
                    episodeIndex = nextEpisode.index,
                    rangeIndex = calculateRangeIndex(matchedSource.episodes.size, nextEpisode.index),
                    restoredByFallback = false
                )
            }
        }

        return PreferredSelection(
            sourceKey = matchedSource.key,
            episodeIndex = targetEpisode.index,
            rangeIndex = calculateRangeIndex(matchedSource.episodes.size, targetEpisode.index),
            restoredByFallback = false
        )
    }

    private fun calculateRangeIndex(totalEpisodes: Int, episodeIndex: Int): Int {
        val rangeSize = when {
            totalEpisodes > 100 -> 30
            totalEpisodes > 40 -> 20
            else -> 12
        }
        return (episodeIndex.coerceAtLeast(0)) / rangeSize
    }

    private fun commit(mutation: DetailContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: DetailContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
