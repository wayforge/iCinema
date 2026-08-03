package com.icinema.pages.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icinema.cast.CastController
import com.icinema.cast.CastMedia
import com.icinema.domain.model.PlayableEpisode
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import com.icinema.pages.player.core.hls.HlsSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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
    private val reducer: DetailReducer,
    private val castController: CastController,
    private val hlsSessionManager: HlsSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailContract.UiState())
    val uiState: StateFlow<DetailContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<DetailContract.UiEffect>()
    val uiEffect: Flow<DetailContract.UiEffect> = _uiEffect.receiveAsFlow()

    private var castDiscoveryJob: Job? = null

    init {
        viewModelScope.launch {
            castController.state.collect { castState ->
                commit(DetailContract.Mutation.CastStateChanged(castState))
            }
        }
    }

    fun handleIntent(intent: DetailContract.UiIntent) {
        when (intent) {
            is DetailContract.UiIntent.LoadVideo -> loadVideo(intent.videoId)
            DetailContract.UiIntent.RetryLoad -> retryLoad()
            is DetailContract.UiIntent.SelectPlaySource -> selectPlaySource(intent.source)
            is DetailContract.UiIntent.SelectRange -> selectRange(intent.range)
            is DetailContract.UiIntent.SelectEpisode -> selectEpisode(intent.episode)
            is DetailContract.UiIntent.OpenCastFlow -> openCastFlow(intent.sourceKey, intent.episodeIndex)
            DetailContract.UiIntent.DismissCastFlow -> {
                commit(DetailContract.Mutation.CastSheetChanged(visible = false))
            }

            DetailContract.UiIntent.RefreshCastDevices -> startCastDiscovery()
            is DetailContract.UiIntent.SelectCastDevice -> castToDevice(intent.deviceId)
            DetailContract.UiIntent.ToggleCastPlayPause -> toggleCastPlayPause()
            DetailContract.UiIntent.StopCasting -> stopCasting()
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

    private fun openCastFlow(sourceKey: String, episodeIndex: Int) {
        val media = buildCastMedia(sourceKey, episodeIndex)
        if (media == null) {
            emitEffect(DetailContract.UiEffect.ShowMessage("当前剧集暂不支持投屏，仅支持 HLS/m3u8"))
            return
        }

        if (_uiState.value.selectedPlaySource != sourceKey) {
            commit(DetailContract.Mutation.PlaySourceChanged(sourceKey))
        }
        if (_uiState.value.selectedEpisode != episodeIndex) {
            commit(DetailContract.Mutation.EpisodeChanged(episodeIndex))
        }
        commit(
            DetailContract.Mutation.CastSheetChanged(
                visible = true,
                sourceKey = sourceKey,
                episodeIndex = episodeIndex
            )
        )
        startCastDiscovery()
    }

    private fun startCastDiscovery() {
        if (castDiscoveryJob?.isActive == true) return
        castDiscoveryJob = viewModelScope.launch {
            castController.startDiscovery()
        }
    }

    private fun castToDevice(deviceId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val sourceKey = state.pendingCastSourceKey ?: state.selectedPlaySource
            val episodeIndex = state.pendingCastEpisodeIndex ?: state.selectedEpisode
            val media = if (sourceKey != null) {
                buildCastMedia(sourceKey, episodeIndex)
            } else {
                null
            }
            if (media == null) {
                emitEffect(DetailContract.UiEffect.ShowMessage("当前剧集暂不支持投屏"))
                return@launch
            }

            castController.cast(deviceId, media)
                .onSuccess {
                    commit(DetailContract.Mutation.CastSheetChanged(visible = false))
                    emitEffect(DetailContract.UiEffect.ShowMessage("已开始投屏"))
                }
                .onFailure { error ->
                    emitEffect(DetailContract.UiEffect.ShowMessage(error.message ?: "投屏失败"))
                }
        }
    }

    private fun toggleCastPlayPause() {
        viewModelScope.launch {
            val result = if (_uiState.value.castState.isPlaying) {
                castController.pause()
            } else {
                castController.play()
            }
            result.onFailure {
                emitEffect(DetailContract.UiEffect.ShowMessage(it.message ?: "投屏控制失败"))
            }
        }
    }

    private fun stopCasting() {
        viewModelScope.launch {
            castController.stopCasting()
                .onSuccess {
                    commit(DetailContract.Mutation.CastSheetChanged(visible = false))
                    emitEffect(DetailContract.UiEffect.ShowMessage("已断开投屏"))
                }
                .onFailure { error ->
                    emitEffect(DetailContract.UiEffect.ShowMessage(error.message ?: "断开投屏失败"))
                }
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
        video: Video,
        latestPlayback: WatchHistoryItem?
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
                rangeIndex = calculateRangeIndex(defaultEpisode.index),
                restoredByFallback = false
            )
        }

        val matchedSource = playSources.firstOrNull { it.key == latestPlayback.sourceKey }
            ?: return PreferredSelection(
                sourceKey = defaultSource.key,
                episodeIndex = defaultEpisode.index,
                rangeIndex = calculateRangeIndex(defaultEpisode.index),
                restoredByFallback = true
            )

        val targetEpisode = matchedSource.episodes.firstOrNull { it.index == latestPlayback.episodeIndex }

        if (targetEpisode == null) {
            val fallbackEpisode = matchedSource.episodes.firstOrNull { it.isHls } ?: matchedSource.episodes.first()
            return PreferredSelection(
                sourceKey = matchedSource.key,
                episodeIndex = fallbackEpisode.index,
                rangeIndex = calculateRangeIndex(fallbackEpisode.index),
                restoredByFallback = true
            )
        }

        if (latestPlayback.completed) {
            val nextEpisode = matchedSource.episodes.getOrNull(targetEpisode.index + 1)
            if (nextEpisode != null) {
                return PreferredSelection(
                    sourceKey = matchedSource.key,
                    episodeIndex = nextEpisode.index,
                    rangeIndex = calculateRangeIndex(nextEpisode.index),
                    restoredByFallback = false
                )
            }
        }

        return PreferredSelection(
            sourceKey = matchedSource.key,
            episodeIndex = targetEpisode.index,
            rangeIndex = calculateRangeIndex(targetEpisode.index),
            restoredByFallback = false
        )
    }

    private fun calculateRangeIndex(episodeIndex: Int): Int {
        return (episodeIndex.coerceAtLeast(0)) / DETAIL_EPISODE_RANGE_SIZE
    }

    private fun commit(mutation: DetailContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: DetailContract.UiEffect) {
        _uiEffect.trySend(effect)
    }

    private fun buildCastMedia(sourceKey: String, episodeIndex: Int): CastMedia? {
        val video = _uiState.value.video ?: return null
        val source = video.playSources.firstOrNull { it.key == sourceKey } ?: return null
        val episode = source.episodes.firstOrNull { it.index == episodeIndex }
            ?: source.episodes.getOrNull(episodeIndex)
            ?: return null
        if (!episode.isHls) return null
        val castUrl = runCatching { hlsSessionManager.prepareCastUrl(episode.url) }.getOrNull() ?: return null

        return CastMedia(
            url = castUrl,
            title = buildCastTitle(video, episode),
            subtitle = source.key,
            imageUrl = video.picThumb?.takeIf { it.isNotBlank() }
                ?: video.pic.takeIf { it.isNotBlank() }
                ?: ""
        )
    }

    private fun buildCastTitle(video: Video, episode: PlayableEpisode): String {
        return listOf(
            video.name.takeIf { it.isNotBlank() },
            episode.title.takeIf { it.isNotBlank() }
        ).filterNotNull()
            .joinToString(" - ")
            .ifBlank { "iCinema" }
    }

    override fun onCleared() {
        castDiscoveryJob?.cancel()
        super.onCleared()
    }
}
