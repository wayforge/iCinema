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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val PROGRESS_LOOP_INTERVAL_MS = 500L
private const val PROGRESS_SAVE_INTERVAL_TICKS = 10
private const val AD_DETECTION_DEDUP_WINDOW_MS = 30_000L
private const val AD_SKIP_MIN_FORWARD_MS = 250L

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

    private val _progress = MutableStateFlow(PlayerContract.ProgressUi())
    val progress: StateFlow<PlayerContract.ProgressUi> = _progress.asStateFlow()

    /**
     * Structural player chrome without high-frequency clock fields.
     * Position ticks must not invalidate this flow.
     */
    val chromeUi: StateFlow<PlayerChromeUi> = uiState
        .map { it.toChromeUi() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _uiState.value.toChromeUi())

    private val _uiEffect = Channel<PlayerContract.UiEffect>()
    val uiEffect: Flow<PlayerContract.UiEffect> = _uiEffect.receiveAsFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(playbackMediaSourceFactory.createMediaSourceFactory())
        .build()

    private var progressJob: Job? = null
    private var castDiscoveryJob: Job? = null
    private var castProgressJob: Job? = null
    private var retriedPlaybackKey: String? = null
    private var shouldRefreshHomeOnExit: Boolean = false
    private var lastDetectedAdSegmentUrl: String? = null
    private var lastDetectedAdAtMs: Long = 0L
    private val scrubbing = AtomicBoolean(false)

    private val playerListener = object : Media3Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Media3Player.STATE_BUFFERING
            val isPlaying = player.isPlaying
            commit(PlayerContract.Mutation.PlaybackChanged(isPlaying, isBuffering))

            if (playbackState == Media3Player.STATE_READY) {
                publishProgress(force = true)
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
            val errorDetail = error.toDisplayDetail()
            if (playbackKey != null && isLikelyPlaybackChainError(error)) {
                if (retriedPlaybackKey != playbackKey) {
                    retriedPlaybackKey = playbackKey
                    val source = state.playSources.firstOrNull { it.key == state.selectedSourceKey }
                    val currentEpisode = source?.episodes?.getOrNull(state.selectedEpisodeIndex)
                    if (source != null && currentEpisode != null) {
                        emitEffect(PlayerContract.UiEffect.ShowMessage("播放链路异常，正在重试当前剧集"))
                        val seekMs = player.currentPosition.coerceAtLeast(0L)
                        viewModelScope.launch {
                            prepareEpisode(
                                sourceKey = source.key,
                                episode = currentEpisode,
                                seekPositionMs = seekMs,
                                playWhenReady = true
                            )
                        }
                        return
                    }
                }
                val message = "当前线路播放异常，已停止自动切换"
                commit(PlayerContract.Mutation.ErrorChanged(message, errorDetail))
                emitEffect(PlayerContract.UiEffect.ShowMessage(message))
                return
            }

            val message = "当前线路播放失败"
            commit(PlayerContract.Mutation.ErrorChanged(message, errorDetail))
            emitEffect(PlayerContract.UiEffect.ShowMessage(message))
        }
    }

    init {
        player.addListener(playerListener)
        viewModelScope.launch {
            castController.state.collect { castState ->
                commit(PlayerContract.Mutation.CastStateChanged(castState))
                if (castState.isCasting) {
                    _progress.value = PlayerContract.ProgressUi(
                        positionMs = castState.currentPositionMs,
                        durationMs = castState.durationMs.coerceAtLeast(0L),
                        bufferedPositionMs = castState.currentPositionMs
                    )
                }
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
                    publishProgress(force = true)
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
                commit(
                    PlayerContract.Mutation.ControlsVisibilityChanged(
                        !_uiState.value.controlsVisible
                    )
                )
            }

            is PlayerContract.UiIntent.SetControlsVisible -> {
                if (_uiState.value.controlsVisible != intent.visible) {
                    commit(PlayerContract.Mutation.ControlsVisibilityChanged(intent.visible))
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
            PlayerContract.UiIntent.ToggleGestureSeek -> toggleGestureSeek()
            PlayerContract.UiIntent.MarkCurrentSegmentAsAd -> markCurrentSegmentAsAd()
            is PlayerContract.UiIntent.GestureSeek -> seekBy(intent.deltaMs)
            PlayerContract.UiIntent.OpenCastFlow -> openCastFlow()
            PlayerContract.UiIntent.RefreshCastDevices -> startCastDiscovery()
            is PlayerContract.UiIntent.SelectCastDevice -> castToDevice(intent.deviceId)
            PlayerContract.UiIntent.StopCasting -> stopCasting()
            PlayerContract.UiIntent.OnLifecycleStart -> Unit
            PlayerContract.UiIntent.OnLifecycleStop -> onStop()
            PlayerContract.UiIntent.DismissPlayerToast -> {
                commit(PlayerContract.Mutation.PlayerToastChanged(null))
            }

            PlayerContract.UiIntent.ScrubStarted -> scrubbing.set(true)
            PlayerContract.UiIntent.ScrubEnded -> {
                scrubbing.set(false)
                publishProgress(force = true)
            }
        }
    }

    private fun load(videoId: Long, requestedSourceKey: String?, requestedEpisodeIndex: Int) {
        viewModelScope.launch {
            commit(PlayerContract.Mutation.LoadStarted(videoId, requestedSourceKey, requestedEpisodeIndex))

            val loadResult = withContext(Dispatchers.IO) {
                bizPort.loadVideo(videoId)
            }
            loadResult
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

                    val resumePosition = withContext(Dispatchers.IO) {
                        loadResumePosition(
                            videoId = videoId,
                            sourceKey = selectedSource.key,
                            episodeIndex = selectedEpisode.index
                        )
                    }

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
            val videoId = state.videoId ?: return@launch
            val resumePosition = withContext(Dispatchers.IO) {
                loadResumePosition(
                    videoId = videoId,
                    sourceKey = source.key,
                    episodeIndex = nextEpisode.index
                )
            }
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
            val videoId = state.videoId ?: return@launch
            val resumePosition = withContext(Dispatchers.IO) {
                loadResumePosition(
                    videoId = videoId,
                    sourceKey = source.key,
                    episodeIndex = episode.index
                )
            }
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
                retriedPlaybackKey = null
                viewModelScope.launch {
                    prepareEpisode(
                        sourceKey = state.selectedSourceKey,
                        episode = state.currentEpisode,
                        seekPositionMs = state.resumePositionMs
                    )
                }
            }
        }
    }

    private fun togglePlayPause() {
        if (_uiState.value.castState.isCasting) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = if (_uiState.value.castState.isPlaying) {
                    castController.pause()
                } else {
                    castController.play()
                }
                result.onFailure {
                    emitEffect(PlayerContract.UiEffect.ShowMessage(it.message ?: "投屏控制失败"))
                }
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
        publishProgress(force = true)
    }

    /**
     * Heavy HLS session prep on IO; ExoPlayer mutations only on Main.
     */
    private suspend fun prepareEpisode(
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
        lastDetectedAdSegmentUrl = null
        lastDetectedAdAtMs = 0L
        val videoId = _uiState.value.videoId ?: 0L

        val playbackUrl = withContext(Dispatchers.IO) {
            preloadCoordinator.clearFor(
                videoId = videoId,
                sourceKey = sourceKey,
                episodeIndex = episode.index
            )
            runCatching { hlsSessionManager.preparePlaybackUrl(episode.url) }
                .getOrElse { episode.url }
        }

        withContext(Dispatchers.Main.immediate) {
            player.stop()
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(playbackUrl)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )
            player.prepare()
            if ((seekPositionMs ?: 0L) > 0L) {
                player.seekTo(seekPositionMs ?: 0L)
            }
            player.playWhenReady = playWhenReady
        }

        if (videoId != 0L) {
            withContext(Dispatchers.IO) {
                schedulePreload(
                    videoId = videoId,
                    sourceKey = sourceKey,
                    currentEpisodeIndex = episode.index
                )
            }
        }
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
        // Capture clock on main (this is already main), then persist off-main.
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.coerceAtLeast(0L)
        viewModelScope.launch {
            saveCurrentProgress(
                clearCompleted = false,
                positionMs = positionMs,
                durationMs = durationMs
            )
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
        publishProgress(force = true)
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

    private fun toggleGestureSeek() {
        val next = !_uiState.value.gestureSeekEnabled
        commit(PlayerContract.Mutation.GestureSeekChanged(next))
        persistPlayerSettings()
    }

    private fun markCurrentSegmentAsAd() {
        val state = _uiState.value
        val episode = state.currentEpisode
        if (episode == null) {
            emitEffect(PlayerContract.UiEffect.ShowMessage("暂无可标记的播放片段"))
            return
        }
        viewModelScope.launch {
            val playbackPositionMs = if (state.castState.isCasting) {
                state.currentPositionMs
            } else {
                withContext(Dispatchers.Main.immediate) {
                    player.currentPosition.coerceAtLeast(state.currentPositionMs)
                }
            }
            // Download/hash TS on IO — never on main.
            val result = withContext(Dispatchers.IO) {
                hlsSessionManager.markCurrentSegmentAsAd(
                    originUrl = episode.url,
                    playbackPositionMs = playbackPositionMs,
                    videoTitle = state.video?.name.orEmpty(),
                    episodeTitle = episode.title
                )
            }
            result.onSuccess { markedSegment ->
                showPlayerToast(markedSegment.message)
            }.onFailure { error ->
                showPlayerToast(error.message ?: "广告标记失败")
            }
        }
    }

    private fun persistPlayerSettings() {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            bizPort.savePlayerSettings(
                PlayerSettings(
                    playbackSpeed = state.playbackSpeed,
                    autoPlayNextEnabled = state.autoPlayNextEnabled,
                    gestureSeekEnabled = state.gestureSeekEnabled
                )
            )
        }
    }

    /**
     * Publish clock to [progress] only. Avoids heavy HLS buffer scans on the UI path.
     */
    private fun publishProgress(force: Boolean = false) {
        if (_uiState.value.castState.isCasting) return
        if (!force && scrubbing.get()) return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val exoBuffered = player.bufferedPosition.coerceAtLeast(0L)
        val next = PlayerContract.ProgressUi(
            positionMs = positionMs,
            durationMs = player.duration.coerceAtLeast(0L),
            bufferedPositionMs = exoBuffered
        )
        if (force || next != _progress.value) {
            _progress.value = next
        }
        // Coarse uiState copy for save/cast helpers only.
        val state = _uiState.value
        if (
            force ||
            kotlin.math.abs(state.currentPositionMs - next.positionMs) >= 2_000L ||
            state.durationMs != next.durationMs
        ) {
            commit(
                PlayerContract.Mutation.PositionChanged(
                    currentPositionMs = next.positionMs,
                    durationMs = next.durationMs,
                    bufferedPositionMs = next.bufferedPositionMs
                )
            )
        }
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        // Entire loop on Default: main only receives lightweight state / seek results.
        progressJob = viewModelScope.launch(Dispatchers.Default) {
            var saveTick = 0
            while (isActive) {
                val snapshot = capturePlaybackSnapshot()
                if (snapshot != null) {
                    publishProgressFromSnapshot(snapshot)
                    val skipAction = resolveAdSkipAction(snapshot)
                    if (skipAction != null) {
                        val recorded = prepareAdSkipRecord(skipAction)
                        withContext(Dispatchers.Main.immediate) {
                            applyAdSkipOnMain(skipAction, recorded)
                        }
                    }
                }
                saveTick += 1
                if (saveTick >= PROGRESS_SAVE_INTERVAL_TICKS && snapshot != null) {
                    saveCurrentProgress(
                        clearCompleted = false,
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs
                    )
                    saveTick = 0
                }
                delay(PROGRESS_LOOP_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private data class PlaybackSnapshot(
        val positionMs: Long,
        val durationMs: Long,
        val bufferedMs: Long,
        val episodeUrl: String,
        val episodeTitle: String,
        val videoTitle: String,
        val isCasting: Boolean,
        val isHls: Boolean
    )

    private data class AdSkipAction(
        val seekToMs: Long,
        val dedupeKey: String,
        val recordOriginPositionMs: Long,
        val episodeUrl: String,
        val episodeTitle: String,
        val videoTitle: String,
        val showToast: Boolean
    )

    private data class AdSkipRecord(
        val candidate: com.icinema.pages.player.core.hls.HlsAdDetectionCandidate?
    )

    /** Player getters must run on main. */
    private suspend fun capturePlaybackSnapshot(): PlaybackSnapshot? {
        val state = _uiState.value
        val episode = state.currentEpisode ?: return null
        if (state.castState.isCasting) return null
        return withContext(Dispatchers.Main.immediate) {
            PlaybackSnapshot(
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
                episodeUrl = episode.url,
                episodeTitle = episode.title,
                videoTitle = state.video?.name.orEmpty(),
                isCasting = state.castState.isCasting,
                isHls = episode.isHls
            )
        }
    }

    private fun publishProgressFromSnapshot(snapshot: PlaybackSnapshot, force: Boolean = false) {
        if (scrubbing.get() && !force) return
        val next = PlayerContract.ProgressUi(
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
            bufferedPositionMs = snapshot.bufferedMs
        )
        if (force || next != _progress.value) {
            _progress.value = next
        }
        val state = _uiState.value
        if (
            force ||
            kotlin.math.abs(state.currentPositionMs - next.positionMs) >= 2_000L ||
            state.durationMs != next.durationMs
        ) {
            commit(
                PlayerContract.Mutation.PositionChanged(
                    currentPositionMs = next.positionMs,
                    durationMs = next.durationMs,
                    bufferedPositionMs = next.bufferedPositionMs
                )
            )
        }
    }

    /**
     * Background-only resolve. No ExoPlayer calls, no UI commits.
     * Each ad range is acted on at most once (dedupe) to avoid seek storms.
     */
    private fun resolveAdSkipAction(snapshot: PlaybackSnapshot): AdSkipAction? {
        if (snapshot.isCasting || !snapshot.isHls) return null
        val playbackPositionMs = snapshot.positionMs
        val now = System.currentTimeMillis()

        val skipRange = hlsSessionManager.resolveAdSkipTarget(snapshot.episodeUrl, playbackPositionMs)
        if (skipRange != null && skipRange.endMs > playbackPositionMs + AD_SKIP_MIN_FORWARD_MS) {
            val rangeKey = "range:${skipRange.startMs}-${skipRange.endMs}"
            if (
                rangeKey == lastDetectedAdSegmentUrl &&
                now - lastDetectedAdAtMs <= AD_DETECTION_DEDUP_WINDOW_MS
            ) {
                return null
            }
            return AdSkipAction(
                seekToMs = skipRange.endMs,
                dedupeKey = rangeKey,
                recordOriginPositionMs = maxOf(playbackPositionMs, skipRange.startMs),
                episodeUrl = snapshot.episodeUrl,
                episodeTitle = snapshot.episodeTitle,
                videoTitle = snapshot.videoTitle,
                showToast = true
            )
        }

        val candidate = hlsSessionManager.resolveAdDetectionCandidate(
            originUrl = snapshot.episodeUrl,
            playbackPositionMs = playbackPositionMs,
            recordHit = false
        ) ?: return null
        if (
            candidate.segmentUrl == lastDetectedAdSegmentUrl &&
            now - lastDetectedAdAtMs <= AD_DETECTION_DEDUP_WINDOW_MS
        ) {
            return null
        }
        val endMs = candidate.segmentEndPositionMs
            ?: (playbackPositionMs + ((candidate.rule.durationSeconds ?: 0.0) * 1000).toLong())
        if (endMs <= playbackPositionMs + AD_SKIP_MIN_FORWARD_MS) return null
        return AdSkipAction(
            seekToMs = endMs,
            dedupeKey = candidate.segmentUrl,
            recordOriginPositionMs = playbackPositionMs,
            episodeUrl = snapshot.episodeUrl,
            episodeTitle = snapshot.episodeTitle,
            videoTitle = snapshot.videoTitle,
            showToast = true
        )
    }

    private fun prepareAdSkipRecord(action: AdSkipAction): AdSkipRecord {
        lastDetectedAdSegmentUrl = action.dedupeKey
        lastDetectedAdAtMs = System.currentTimeMillis()
        val candidate = hlsSessionManager.resolveAdDetectionCandidate(
            originUrl = action.episodeUrl,
            playbackPositionMs = action.recordOriginPositionMs,
            recordHit = true
        )
        if (candidate != null) {
            hlsSessionManager.recordDetectedSegment(
                candidate = candidate,
                videoTitle = action.videoTitle,
                episodeTitle = action.episodeTitle
            )
        }
        return AdSkipRecord(candidate)
    }

    /** Main-thread only: ExoPlayer seek + UI toast/progress. Recording already done off-main. */
    private fun applyAdSkipOnMain(action: AdSkipAction, @Suppress("UNUSED_PARAMETER") record: AdSkipRecord) {
        player.seekTo(action.seekToMs)
        publishProgress(force = true)
        if (action.showToast) {
            showPlayerToast("已跳过广告")
        }
    }

    private fun showPlayerToast(message: String) {
        // In-player overlay only — avoid duplicate Snackbar toast.
        commit(PlayerContract.Mutation.PlayerToastChanged(message))
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
            val media = withContext(Dispatchers.IO) {
                buildCurrentCastMedia()
            } ?: run {
                emitEffect(PlayerContract.UiEffect.ShowMessage("当前视频不能投屏"))
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                castController.cast(deviceId, media)
            }
            result.onSuccess {
                withContext(Dispatchers.Main.immediate) {
                    player.pause()
                }
                startCastProgressUpdates()
                emitEffect(PlayerContract.UiEffect.ShowMessage("已开始投屏"))
            }.onFailure { error ->
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
        viewModelScope.launch(Dispatchers.IO) {
            castController.seekTo(target)
                .onFailure {
                    emitEffect(PlayerContract.UiEffect.ShowMessage(it.message ?: "投屏控制失败"))
                }
        }
    }

    private fun stopCasting() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                castController.stopCasting()
            }
            result.onSuccess { remotePosition ->
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
            }.onFailure { error ->
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
        val castUrl = runCatching { hlsSessionManager.prepareCastUrl(episode.url) }.getOrNull() ?: return null
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

    /**
     * Never touch [player] here — callers must pass clock values already read on Main,
     * or leave them null to use [progress]/[uiState] snapshots only.
     */
    private suspend fun saveCurrentProgress(
        clearCompleted: Boolean,
        positionMs: Long? = null,
        durationMs: Long? = null
    ) {
        val state = _uiState.value
        val videoId = state.videoId ?: return
        val sourceKey = state.selectedSourceKey ?: return
        val episode = state.currentEpisode ?: return

        if (clearCompleted) {
            withContext(Dispatchers.IO) {
                bizPort.markProgressCompleted(videoId, sourceKey, episode.index)
            }
            shouldRefreshHomeOnExit = true
            return
        }

        val progress = _progress.value
        val resolvedDuration = when {
            durationMs != null && durationMs > 0L -> durationMs
            state.castState.isCasting -> state.durationMs
            progress.durationMs > 0L -> progress.durationMs
            else -> state.durationMs
        }
        val resolvedPosition = when {
            positionMs != null && positionMs > 0L -> positionMs
            state.castState.isCasting -> state.currentPositionMs
            progress.positionMs > 0L -> progress.positionMs
            else -> state.currentPositionMs
        }
        if (resolvedDuration <= 0L || resolvedPosition <= 0L) return

        withContext(Dispatchers.IO) {
            bizPort.saveProgress(
                videoId = videoId,
                videoName = state.video?.name.orEmpty(),
                videoPic = state.video?.pic.orEmpty(),
                sourceKey = sourceKey,
                episodeIndex = episode.index,
                episodeTitle = episode.title,
                positionMs = resolvedPosition,
                durationMs = resolvedDuration
            )
        }
        shouldRefreshHomeOnExit = true
    }

    private fun buildPlaybackKey(state: PlayerContract.UiState): String? {
        val videoId = state.videoId ?: return null
        val source = state.selectedSourceKey ?: return null
        return "$videoId:$source:${state.selectedEpisodeIndex}"
    }

    private fun isLikelyPlaybackChainError(error: PlaybackException): Boolean {
        val texts = buildList {
            var current: Throwable? = error
            while (current != null) {
                add(current.javaClass.name)
                add(current.message.orEmpty())
                current = current.cause
            }
        }.joinToString(" | ").lowercase()
        return texts.contains("127.0.0.1") ||
            texts.contains("localhost") ||
            texts.contains("hlsproxy") ||
            texts.contains("cache") ||
            texts.contains("response code: 502") ||
            texts.contains("response code=502") ||
            texts.contains("response code: 503") ||
            texts.contains("response code=503") ||
            texts.contains("manifest temporarily unavailable") ||
            texts.contains("temporarily unavailable") ||
            texts.contains("resource request failed") ||
            texts.contains("broken pipe") ||
            texts.contains("connection reset") ||
            texts.contains("socket closed")
    }

    private fun PlaybackException.toDisplayDetail(): String {
        return buildList {
            var current: Throwable? = this@toDisplayDetail
            while (current != null) {
                val type = current.javaClass.simpleName.ifBlank { current.javaClass.name }
                val message = current.message
                    ?.replace(Regex("""([?&][^=]+=)[^&\\s]+"""), "${'$'}1***")
                    ?.take(800)
                    .orEmpty()
                add(if (message.isBlank()) type else "$type: $message")
                current = current.cause
            }
        }.distinct().joinToString(separator = "\n")
    }

    private fun commit(mutation: PlayerContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
        when (mutation) {
            is PlayerContract.Mutation.LoadStarted,
            is PlayerContract.Mutation.SourceSelected,
            is PlayerContract.Mutation.EpisodeSelected -> {
                _progress.value = PlayerContract.ProgressUi()
            }
            else -> Unit
        }
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
