package com.icinema.pages.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player as Media3Player
import androidx.media3.exoplayer.ExoPlayer
import com.icinema.cast.CastController
import com.icinema.cast.CastMedia
import com.icinema.pages.player.core.PlaybackMediaSourceFactory
import com.icinema.pages.player.core.PlayerPreloadCoordinator
import com.icinema.pages.player.core.hls.HlsSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val bizPort: PlayerBizPort,
    private val reducer: PlayerReducer,
    private val playbackMediaSourceFactory: PlaybackMediaSourceFactory,
    private val preloadCoordinator: PlayerPreloadCoordinator,
    private val hlsSessionManager: HlsSessionManager,
    private val castController: CastController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerContract.UiState())
    val uiState: StateFlow<PlayerContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<PlayerContract.UiEffect>()
    val uiEffect: Flow<PlayerContract.UiEffect> = _uiEffect.receiveAsFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(playbackMediaSourceFactory.createMediaSourceFactory())
        .build()

    private var progressJob: Job? = null
    private var castDiscoveryJob: Job? = null
    private var castProgressJob: Job? = null
    private var autoSwitchedForPlaybackKey: String? = null
    private var shouldRefreshHomeOnExit: Boolean = false

    private val playerListener = object : Media3Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Media3Player.STATE_BUFFERING
            val isPlaying = player.isPlaying
            commit(PlayerContract.Mutation.PlaybackChanged(isPlaying, isBuffering))

            if (playbackState == Media3Player.STATE_READY) {
                updatePlaybackPosition()
            }

            if (playbackState == Media3Player.STATE_ENDED) {
                viewModelScope.launch {
                    onPlaybackCompleted()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            commit(
                PlayerContract.Mutation.PlaybackChanged(
                    isPlaying = isPlaying,
                    isBuffering = player.playbackState == Media3Player.STATE_BUFFERING
                )
            )
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onPlayerError(error: PlaybackException) {
            val state = _uiState.value
            val playbackKey = buildPlaybackKey(state)
            if (playbackKey != null && autoSwitchedForPlaybackKey != playbackKey) {
                val source = state.playSources.firstOrNull { it.key == state.selectedSourceKey }
                val currentEpisode = source?.episodes?.getOrNull(state.selectedEpisodeIndex)
                if (source != null && currentEpisode != null) {
                    val alternative = source.episodes.firstOrNull { it.index != currentEpisode.index && it.isHls }
                    if (alternative != null) {
                        autoSwitchedForPlaybackKey = playbackKey
                        handleIntent(PlayerContract.UiIntent.SelectEpisode(alternative.index))
                        emitEffect(PlayerContract.UiEffect.ShowMessage("当前线路异常，已自动切换可用剧集重试"))
                        return
                    }
                }
            }
            val message = error.message ?: "播放失败"
            commit(PlayerContract.Mutation.ErrorChanged(message))
            emitEffect(PlayerContract.UiEffect.ShowMessage(message))
        }
    }

    init {
        player.addListener(playerListener)
        viewModelScope.launch {
            castController.state.collect { castState ->
                commit(PlayerContract.Mutation.CastStateChanged(castState))
            }
        }
        viewModelScope.launch {
            val settings = bizPort.loadPlayerSettings()
            commit(
                PlayerContract.Mutation.SettingsLoaded(
                    playbackSpeed = settings.playbackSpeed,
                    autoPlayNextEnabled = settings.autoPlayNextEnabled,
                    gestureSeekEnabled = settings.gestureSeekEnabled
                )
            )
            player.setPlaybackSpeed(settings.playbackSpeed)
        }
    }

    fun handleIntent(intent: PlayerContract.UiIntent) {
        when (intent) {
            is PlayerContract.UiIntent.Load -> load(intent.videoId, intent.sourceKey, intent.episodeIndex)
            PlayerContract.UiIntent.TogglePlayPause -> togglePlayPause()
            is PlayerContract.UiIntent.SeekTo -> {
                if (_uiState.value.castState.isCasting) {
                    seekCastTo(intent.positionMs)
                } else {
                    player.seekTo(intent.positionMs)
                    updatePlaybackPosition()
                }
            }

            PlayerContract.UiIntent.SeekForward -> seekBy(10_000L)
            PlayerContract.UiIntent.SeekBackward -> seekBy(-10_000L)
            is PlayerContract.UiIntent.SelectSource -> selectSource(intent.sourceKey)
            is PlayerContract.UiIntent.SelectEpisode -> selectEpisode(intent.episodeIndex)
            PlayerContract.UiIntent.PlayNext -> playNext()
            PlayerContract.UiIntent.PlayPrevious -> playPrevious()
            PlayerContract.UiIntent.Retry -> retry()
            PlayerContract.UiIntent.ToggleControls -> {
                if (!_uiState.value.controlsLocked) {
                    commit(
                        PlayerContract.Mutation.ControlsVisibilityChanged(
                            !_uiState.value.controlsVisible
                        )
                    )
                }
            }

            is PlayerContract.UiIntent.OpenSheet -> {
                commit(PlayerContract.Mutation.SheetModeChanged(intent.mode))
            }

            PlayerContract.UiIntent.DismissSheet -> {
                commit(PlayerContract.Mutation.SheetModeChanged(null))
            }

            PlayerContract.UiIntent.EnterFullscreen -> {
                commit(PlayerContract.Mutation.FullscreenChanged(true))
            }

            PlayerContract.UiIntent.ExitFullscreen -> {
                commit(PlayerContract.Mutation.FullscreenChanged(true))
            }

            PlayerContract.UiIntent.AcceptResume -> {
                commit(PlayerContract.Mutation.ResumePositionChanged(null))
            }

            PlayerContract.UiIntent.RestartFromBeginning -> restartFromBeginning()
            is PlayerContract.UiIntent.SetPlaybackSpeed -> setPlaybackSpeed(intent.speed)
            PlayerContract.UiIntent.ToggleAutoPlayNext -> toggleAutoPlayNext()
            PlayerContract.UiIntent.ToggleControlsLock -> toggleControlsLock()
            PlayerContract.UiIntent.ToggleGestureSeek -> toggleGestureSeek()
            is PlayerContract.UiIntent.GestureSeek -> seekBy(intent.deltaMs)
            PlayerContract.UiIntent.OpenCastFlow -> openCastFlow()
            PlayerContract.UiIntent.RefreshCastDevices -> startCastDiscovery()
            is PlayerContract.UiIntent.SelectCastDevice -> castToDevice(intent.deviceId)
            PlayerContract.UiIntent.StopCasting -> stopCasting()
            PlayerContract.UiIntent.OnLifecycleStart -> Unit
            PlayerContract.UiIntent.OnLifecycleStop -> onStop()
        }
    }

    private fun load(videoId: Long, requestedSourceKey: String?, requestedEpisodeIndex: Int) {
        viewModelScope.launch {
            commit(PlayerContract.Mutation.LoadStarted(videoId, requestedSourceKey, requestedEpisodeIndex))

            bizPort.loadVideo(videoId)
                .onSuccess { video ->
                    val sources = video.toPlaySources()
                    if (sources.isEmpty()) {
                        commit(PlayerContract.Mutation.LoadFailed("当前视频没有可用的播放源"))
                        return@onSuccess
                    }

                    val defaultSource = sources.firstOrNull { source ->
                        source.episodes.any { it.isHls }
                    } ?: sources.first()

                    val selectedSource =
                        requestedSourceKey?.let { key -> sources.firstOrNull { it.key == key } }
                            ?: defaultSource

                    val selectedEpisode = if (requestedSourceKey == null) {
                        selectedSource.episodes.firstOrNull { it.isHls }
                            ?: selectedSource.episodes.first()
                    } else {
                        selectedSource.episodes.getOrElse(
                            requestedEpisodeIndex.coerceIn(0, selectedSource.episodes.lastIndex)
                        ) { selectedSource.episodes.first() }
                    }

                    val resumePosition = loadResumePosition(
                        videoId = videoId,
                        sourceKey = selectedSource.key,
                        episodeIndex = selectedEpisode.index
                    )

                    commit(
                        PlayerContract.Mutation.LoadSucceeded(
                            videoId = videoId,
                            video = video,
                            playSources = sources,
                            sourceKey = selectedSource.key,
                            episodeIndex = selectedEpisode.index,
                            currentEpisode = selectedEpisode,
                            resumePositionMs = resumePosition
                        )
                    )

                    prepareEpisode(selectedSource.key, selectedEpisode, resumePosition)
                }
                .onFailure { error ->
                    commit(PlayerContract.Mutation.LoadFailed(error.message ?: "视频加载失败"))
                }
        }
    }

    private fun selectSource(sourceKey: String) {
        val state = _uiState.value
        val source = state.playSources.firstOrNull { it.key == sourceKey } ?: return
        val nextEpisode = source.episodes.getOrElse(
            state.selectedEpisodeIndex.coerceIn(0, source.episodes.lastIndex)
        ) { source.episodes.first() }

        viewModelScope.launch {
            val resumePosition = loadResumePosition(
                videoId = state.videoId ?: return@launch,
                sourceKey = source.key,
                episodeIndex = nextEpisode.index
            )
            commit(
                PlayerContract.Mutation.SourceSelected(
                    sourceKey = source.key,
                    episodeIndex = nextEpisode.index,
                    currentEpisode = nextEpisode,
                    canPlayNext = nextEpisode.index < source.episodes.lastIndex
                )
            )
            commit(PlayerContract.Mutation.ResumePositionChanged(resumePosition))
            prepareEpisode(
                source.key,
                nextEpisode,
                resumePosition,
                playWhenReady = !_uiState.value.castState.isCasting
            )
            castCurrentMediaIfNeeded()
        }
    }

    private fun selectEpisode(episodeIndex: Int) {
        val state = _uiState.value
        val source = state.playSources.firstOrNull { it.key == state.selectedSourceKey } ?: return
        val episode = source.episodes.getOrNull(episodeIndex) ?: return

        viewModelScope.launch {
            val resumePosition = loadResumePosition(
                videoId = state.videoId ?: return@launch,
                sourceKey = source.key,
                episodeIndex = episode.index
            )
            commit(
                PlayerContract.Mutation.EpisodeSelected(
                    episodeIndex = episode.index,
                    currentEpisode = episode,
                    canPlayNext = episode.index < source.episodes.lastIndex
                )
            )
            commit(PlayerContract.Mutation.ResumePositionChanged(resumePosition))
            prepareEpisode(
                source.key,
                episode,
                resumePosition,
                playWhenReady = !_uiState.value.castState.isCasting
            )
            castCurrentMediaIfNeeded()
        }
    }

    private fun playNext() {
        val state = _uiState.value
        val source = state.playSources.firstOrNull { it.key == state.selectedSourceKey } ?: return
        val nextEpisode = source.episodes.getOrNull(state.selectedEpisodeIndex + 1) ?: return
        handleIntent(PlayerContract.UiIntent.SelectEpisode(nextEpisode.index))
    }

    private fun playPrevious() {
        val previousIndex = (_uiState.value.selectedEpisodeIndex - 1).coerceAtLeast(0)
        handleIntent(PlayerContract.UiIntent.SelectEpisode(previousIndex))
    }

    private fun retry() {
        val state = _uiState.value
        when {
            state.video == null && state.videoId != null -> {
                handleIntent(
                    PlayerContract.UiIntent.Load(
                        state.videoId,
                        state.selectedSourceKey,
                        state.selectedEpisodeIndex
                    )
                )
            }

            state.currentEpisode != null && state.selectedSourceKey != null -> {
                if (state.castState.isCasting) {
                    castCurrentMediaIfNeeded()
                    return
                }
                prepareEpisode(
                    sourceKey = state.selectedSourceKey,
                    episode = state.currentEpisode,
                    seekPositionMs = state.resumePositionMs
                )
            }
        }
    }

    private fun togglePlayPause() {
        if (_uiState.value.castState.isCasting) {
            viewModelScope.launch {
                val result = if (_uiState.value.castState.isPlaying) {
                    castController.pause()
                } else {
                    castController.play()
                }
                result.onFailure { emitEffect(PlayerContract.UiEffect.ShowMessage(it.message ?: "投屏控制失败")) }
            }
            return
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    private fun seekBy(deltaMs: Long) {
        if (_uiState.value.castState.isCasting) {
            seekCastTo(_uiState.value.currentPositionMs + deltaMs)
            return
        }

        val newPosition = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(newPosition)
        updatePlaybackPosition()
    }

    private fun prepareEpisode(
        sourceKey: String,
        episode: com.icinema.domain.model.PlayableEpisode,
        seekPositionMs: Long?,
        playWhenReady: Boolean = true
    ) {
        if (!episode.isHls) {
            val message = "当前版本仅支持 HLS 播放源"
            commit(PlayerContract.Mutation.ErrorChanged(message))
            emitEffect(PlayerContract.UiEffect.ShowMessage(message))
            return
        }

        commit(PlayerContract.Mutation.ErrorChanged(null))
        val playbackUrl = runCatching { hlsSessionManager.playbackUrl(episode.url) }
            .getOrElse { error ->
                val message = error.message ?: "播放代理启动失败"
                commit(PlayerContract.Mutation.ErrorChanged(message))
                emitEffect(PlayerContract.UiEffect.ShowMessage(message))
                return
            }
        player.stop()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(playbackUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        )
        hlsSessionManager.prefetch(episode.url)
        player.prepare()
        if ((seekPositionMs ?: 0L) > 0L) {
            player.seekTo(seekPositionMs ?: 0L)
        }
        player.playWhenReady = playWhenReady

        schedulePreload(
            videoId = _uiState.value.videoId ?: return,
            sourceKey = sourceKey,
            currentEpisodeIndex = episode.index
        )
    }

    private fun schedulePreload(videoId: Long, sourceKey: String, currentEpisodeIndex: Int) {
        val source = _uiState.value.playSources.firstOrNull { it.key == sourceKey }
        val nextEpisode = source?.episodes?.getOrNull(currentEpisodeIndex + 1)
        preloadCoordinator.preload(videoId, sourceKey, nextEpisode)
    }

    private suspend fun loadResumePosition(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ): Long? {
        val progress = bizPort.loadSavedProgress(videoId, sourceKey, episodeIndex) ?: return null
        if (progress.positionMs < 30_000L) return null
        if (progress.durationMs > 0 && progress.positionMs >= progress.durationMs * 0.95f) return null
        return progress.positionMs
    }

    private suspend fun onPlaybackCompleted() {
        saveCurrentProgress(clearCompleted = true)
        if (_uiState.value.autoPlayNextEnabled) {
            playNext()
        }
    }

    private fun onStop() {
        if (!_uiState.value.castState.isCasting) {
            player.pause()
        }
        viewModelScope.launch {
            saveCurrentProgress(clearCompleted = false)
        }
    }

    fun consumeHomeRefreshSignal(): Boolean {
        val shouldRefresh = shouldRefreshHomeOnExit
        shouldRefreshHomeOnExit = false
        return shouldRefresh
    }

    private fun restartFromBeginning() {
        player.seekTo(0L)
        commit(PlayerContract.Mutation.ResumePositionChanged(null))
        updatePlaybackPosition()
    }

    private fun setPlaybackSpeed(speed: Float) {
        val nextSpeed = speed.coerceIn(0.75f, 2.0f)
        player.setPlaybackSpeed(nextSpeed)
        commit(PlayerContract.Mutation.PlaybackSpeedChanged(nextSpeed))
        persistPlayerSettings()
    }

    private fun toggleAutoPlayNext() {
        val next = !_uiState.value.autoPlayNextEnabled
        commit(PlayerContract.Mutation.AutoPlayNextChanged(next))
        persistPlayerSettings()
    }

    private fun toggleControlsLock() {
        val nextLocked = !_uiState.value.controlsLocked
        commit(PlayerContract.Mutation.ControlsLockedChanged(nextLocked))
        commit(PlayerContract.Mutation.ControlsVisibilityChanged(true))
    }

    private fun toggleGestureSeek() {
        val next = !_uiState.value.gestureSeekEnabled
        commit(PlayerContract.Mutation.GestureSeekChanged(next))
        persistPlayerSettings()
    }

    private fun persistPlayerSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            bizPort.savePlayerSettings(
                PlayerSettings(
                    playbackSpeed = state.playbackSpeed,
                    autoPlayNextEnabled = state.autoPlayNextEnabled,
                    gestureSeekEnabled = state.gestureSeekEnabled
                )
            )
        }
    }

    private fun updatePlaybackPosition() {
        if (_uiState.value.castState.isCasting) return
        commit(
            PlayerContract.Mutation.PositionChanged(
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
            )
        )
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackPosition()
                saveCurrentProgress(clearCompleted = false)
                delay(5_000L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun openCastFlow() {
        commit(PlayerContract.Mutation.SheetModeChanged(PlayerContract.SheetMode.CastDevices))
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
            val media = buildCurrentCastMedia() ?: run {
                emitEffect(PlayerContract.UiEffect.ShowMessage("当前视频不能投屏"))
                return@launch
            }
            castController.cast(deviceId, media)
                .onSuccess {
                    player.pause()
                    startCastProgressUpdates()
                    emitEffect(PlayerContract.UiEffect.ShowMessage("已开始投屏"))
                }
                .onFailure { error ->
                    emitEffect(PlayerContract.UiEffect.ShowMessage(error.message ?: "投屏失败"))
                }
        }
    }

    private fun castCurrentMediaIfNeeded() {
        val deviceId = _uiState.value.castState.connectedDevice?.id ?: return
        castToDevice(deviceId)
    }

    private fun seekCastTo(positionMs: Long) {
        val duration = _uiState.value.durationMs.coerceAtLeast(0L)
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        viewModelScope.launch {
            castController.seekTo(target)
                .onFailure { emitEffect(PlayerContract.UiEffect.ShowMessage(it.message ?: "投屏控制失败")) }
        }
    }

    private fun stopCasting() {
        viewModelScope.launch {
            castController.stopCasting()
                .onSuccess { remotePosition ->
                    stopCastProgressUpdates()
                    val state = _uiState.value
                    val episode = state.currentEpisode
                    if (episode != null && state.selectedSourceKey != null) {
                        prepareEpisode(
                            sourceKey = state.selectedSourceKey,
                            episode = episode,
                            seekPositionMs = remotePosition ?: state.currentPositionMs,
                            playWhenReady = true
                        )
                    }
                    emitEffect(PlayerContract.UiEffect.ShowMessage("已停止投屏"))
                }
                .onFailure { error ->
                    emitEffect(PlayerContract.UiEffect.ShowMessage(error.message ?: "停止投屏失败"))
                }
        }
    }

    private fun startCastProgressUpdates() {
        if (castProgressJob?.isActive == true) return
        castProgressJob = viewModelScope.launch {
            while (isActive && castController.state.value.isCasting) {
                castController.refreshPlaybackPosition()
                saveCurrentProgress(clearCompleted = false)
                delay(5_000L)
            }
        }
    }

    private fun stopCastProgressUpdates() {
        castProgressJob?.cancel()
        castProgressJob = null
    }

    private fun buildCurrentCastMedia(): CastMedia? {
        val state = _uiState.value
        val episode = state.currentEpisode ?: return null
        if (!episode.isHls) return null
        val castUrl = runCatching { hlsSessionManager.castUrl(episode.url) }.getOrNull() ?: return null
        return CastMedia(
            url = castUrl,
            title = listOfNotNull(
                state.video?.name?.takeIf { it.isNotBlank() },
                episode.title.takeIf { it.isNotBlank() }
            ).joinToString(" - ").ifBlank { "iCinema" },
            subtitle = state.selectedSourceKey.orEmpty(),
            imageUrl = state.video?.pic.orEmpty(),
            positionMs = state.currentPositionMs,
            durationMs = state.durationMs
        )
    }

    private suspend fun saveCurrentProgress(clearCompleted: Boolean) {
        val state = _uiState.value
        val videoId = state.videoId ?: return
        val sourceKey = state.selectedSourceKey ?: return
        val episode = state.currentEpisode ?: return

        if (clearCompleted) {
            bizPort.markProgressCompleted(videoId, sourceKey, episode.index)
            shouldRefreshHomeOnExit = true
            return
        }

        val durationMs = if (state.castState.isCasting) {
            state.durationMs
        } else {
            player.duration.takeIf { it > 0 } ?: state.durationMs
        }
        val positionMs = if (state.castState.isCasting) {
            state.currentPositionMs
        } else {
            player.currentPosition.coerceAtLeast(0L)
        }
        if (durationMs <= 0L || positionMs <= 0L) return

        bizPort.saveProgress(
            videoId = videoId,
            videoName = state.video?.name.orEmpty(),
            videoPic = state.video?.pic.orEmpty(),
            sourceKey = sourceKey,
            episodeIndex = episode.index,
            episodeTitle = episode.title,
            positionMs = positionMs,
            durationMs = durationMs
        )
        shouldRefreshHomeOnExit = true
    }

    private fun buildPlaybackKey(state: PlayerContract.UiState): String? {
        val videoId = state.videoId ?: return null
        val source = state.selectedSourceKey ?: return null
        return "$videoId:$source:${state.selectedEpisodeIndex}"
    }

    private fun commit(mutation: PlayerContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: PlayerContract.UiEffect) {
        _uiEffect.trySend(effect)
    }

    override fun onCleared() {
        stopProgressUpdates()
        stopCastProgressUpdates()
        castDiscoveryJob?.cancel()
        preloadCoordinator.release()
        player.removeListener(playerListener)
        player.release()
        super.onCleared()
    }
}
