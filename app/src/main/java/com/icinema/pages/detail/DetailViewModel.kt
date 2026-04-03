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
            DetailContract.UiIntent.ClearVideo -> clearVideo()
        }
    }

    private fun loadVideo(videoId: Long) {
        viewModelScope.launch {
            commit(DetailContract.Mutation.LoadStarted(videoId))
            bizPort.loadVideo(videoId)
                .onSuccess { video ->
                    commit(
                        DetailContract.Mutation.LoadSucceeded(
                            videoId = videoId,
                            video = video,
                            preferredSource = video.playGroups.firstOrNull()?.first
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

    private fun clearVideo() {
        commit(DetailContract.Mutation.VideoCleared)
    }

    private fun commit(mutation: DetailContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: DetailContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
