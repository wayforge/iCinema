package com.icinema.pages.player

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.icinema.R
import com.icinema.ui.theme.iCinemaTheme
import kotlin.math.abs
import kotlinx.coroutines.delay

private val ControlsEnter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 3 }
private val ControlsExit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 3 }
private val BottomEnter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 3 }
private val BottomExit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 3 }

@Composable
internal fun PlayerSurfaceSection(
    chrome: PlayerChromeUi,
    progress: PlayerContract.ProgressUi,
    player: ExoPlayer?,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    var showErrorDetails by remember(chrome.errorDetail) { mutableStateOf(false) }
    val touchSlop = LocalViewConfiguration.current.touchSlop

    // Local-first visibility for instant UI; keep in sync with VM.
    var controlsVisible by remember { mutableStateOf(chrome.controlsVisible) }
    LaunchedEffect(chrome.controlsVisible) {
        controlsVisible = chrome.controlsVisible
    }

    val latestOnIntent by rememberUpdatedState(onIntent)
    val latestGestureSeek by rememberUpdatedState(chrome.gestureSeekEnabled)
    val latestVisible by rememberUpdatedState(controlsVisible)

    Box(
        modifier = modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        PlayerRuntimeSurface(
            player = player,
            isPreview = isPreview,
            modifier = Modifier.fillMaxSize()
        )

        if (chrome.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // Full-screen scrim under chrome: tap toggles; horizontal drag seeks.
        PlayerInteractionScrim(
            controlsVisible = controlsVisible,
            gestureSeekEnabled = latestGestureSeek,
            touchSlop = touchSlop,
            onToggleControls = {
                val next = !latestVisible
                controlsVisible = next
                latestOnIntent(PlayerContract.UiIntent.SetControlsVisible(next))
            },
            onGestureSeek = { deltaMs ->
                latestOnIntent(PlayerContract.UiIntent.GestureSeek(deltaMs))
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = ControlsEnter,
            exit = ControlsExit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            PlayerTopBar(
                title = formatPlayerTitle(
                    videoTitle = chrome.video?.name.orEmpty(),
                    episodeTitle = chrome.currentEpisode?.title.orEmpty()
                ),
                isCasting = chrome.castState.isCasting || chrome.castState.isConnecting,
                canCast = chrome.currentEpisode?.isHls == true,
                playbackSpeed = chrome.playbackSpeed,
                autoPlayNextEnabled = chrome.autoPlayNextEnabled,
                gestureSeekEnabled = chrome.gestureSeekEnabled,
                onBackClick = onBackClick,
                onIntent = latestOnIntent,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = BottomEnter,
            exit = BottomExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerTransportControls(
                    isPlaying = chrome.isPlaying,
                    onIntent = latestOnIntent
                )
                PlayerTimeline(
                    progress = progress,
                    onIntent = latestOnIntent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        PlayerToastOverlay(
            message = chrome.playerToast,
            token = chrome.playerToastToken,
            onDismiss = { latestOnIntent(PlayerContract.UiIntent.DismissPlayerToast) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp)
        )

        if (chrome.resumePositionMs != null) {
            ResumePrompt(
                positionMs = chrome.resumePositionMs,
                onContinue = { latestOnIntent(PlayerContract.UiIntent.AcceptResume) },
                onRestart = { latestOnIntent(PlayerContract.UiIntent.RestartFromBeginning) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        if (!chrome.isLoading && chrome.error != null) {
            PlayerErrorCard(
                message = chrome.error,
                onRetry = { latestOnIntent(PlayerContract.UiIntent.Retry) },
                onShowDetails = chrome.errorDetail?.let { { showErrorDetails = true } },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }

        if (showErrorDetails && chrome.errorDetail != null) {
            AlertDialog(
                onDismissRequest = { showErrorDetails = false },
                title = { Text("播放错误详情") },
                text = {
                    Text(
                        text = chrome.errorDetail,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showErrorDetails = false }) { Text("关闭") }
                }
            )
        }
    }
}

@Composable
private fun PlayerInteractionScrim(
    controlsVisible: Boolean,
    gestureSeekEnabled: Boolean,
    touchSlop: Float,
    onToggleControls: () -> Unit,
    onGestureSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimAlpha = if (controlsVisible) 1f else 0f
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.48f * dimAlpha),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.56f * dimAlpha)
                    )
                )
            )
            // Transparent hit target always present so Surface never wins touches.
            .background(Color.Transparent)
            .pointerInput(gestureSeekEnabled, touchSlop) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var totalDragX = 0f
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!dragging) {
                                onToggleControls()
                            }
                            change.consume()
                            break
                        }
                        val dx = change.positionChange().x
                        totalDragX += dx
                        if (!dragging && abs(totalDragX) >= touchSlop) {
                            dragging = true
                        }
                        if (dragging && gestureSeekEnabled && dx != 0f) {
                            val delta = (dx * 180L).toLong()
                            if (delta != 0L) {
                                onGestureSeek(delta)
                            }
                        }
                        change.consume()
                    }
                }
            }
    )
}

@Composable
private fun PlayerRuntimeSurface(
    player: ExoPlayer?,
    isPreview: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPreview || player == null) {
        PreviewPlayerPlaceholder(modifier = modifier)
        return
    }

    AndroidView(
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.player_view_texture, null) as PlayerView).apply {
                useController = false
                controllerAutoShow = false
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            view.useController = false
            view.isClickable = false
            view.isFocusable = false
        },
        modifier = modifier
    )
}

@Composable
private fun PreviewPlayerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF090909), Color(0xFF181818), Color(0xFF101A22))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.28f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "Player Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Preview 模式下使用静态占位，不依赖真实播放器 runtime。",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PlayerErrorCard(
    message: String,
    onRetry: () -> Unit,
    onShowDetails: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.76f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重试")
                }
                if (onShowDetails != null) {
                    TextButton(onClick = onShowDetails) {
                        Text("查看错误")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    isCasting: Boolean,
    canCast: Boolean,
    playbackSpeed: Float,
    autoPlayNextEnabled: Boolean,
    gestureSeekEnabled: Boolean,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { onIntent(PlayerContract.UiIntent.MarkCurrentSegmentAsAd) }) {
                Icon(
                    imageVector = Icons.Filled.Report,
                    contentDescription = "标记广告",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = { onIntent(PlayerContract.UiIntent.OpenCastFlow) },
                enabled = canCast || isCasting
            ) {
                Icon(
                    imageVector = if (isCasting) Icons.Filled.CastConnected else Icons.Filled.Cast,
                    contentDescription = if (isCasting) "投屏中" else "投屏",
                    tint = if (canCast || isCasting) Color.White else Color.White.copy(alpha = 0.36f)
                )
            }
            IconButton(onClick = { showSettings = !showSettings }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = if (showSettings) "收起设置" else "播放设置",
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it / 4 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 4 },
            modifier = Modifier.align(Alignment.End)
        ) {
            PlayerSettingsInlineRow(
                playbackSpeed = playbackSpeed,
                autoPlayNextEnabled = autoPlayNextEnabled,
                gestureSeekEnabled = gestureSeekEnabled,
                onSelectSpeed = { speed ->
                    onIntent(PlayerContract.UiIntent.SetPlaybackSpeed(speed))
                },
                onToggleAutoPlayNext = {
                    onIntent(PlayerContract.UiIntent.ToggleAutoPlayNext)
                },
                onToggleGestureSeek = {
                    onIntent(PlayerContract.UiIntent.ToggleGestureSeek)
                },
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = 4.dp, bottom = 2.dp)
            )
        }
    }
}

/** Compact inline settings under the 更多 button — wrap content, not full width. */
@Composable
private fun PlayerSettingsInlineRow(
    playbackSpeed: Float,
    autoPlayNextEnabled: Boolean,
    gestureSeekEnabled: Boolean,
    onSelectSpeed: (Float) -> Unit,
    onToggleAutoPlayNext: () -> Unit,
    onToggleGestureSeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.62f), shape = shape)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = shape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                val selected = playbackSpeed == speed
                val label = if (speed == speed.toInt().toFloat()) {
                    "${speed.toInt()}.0x"
                } else {
                    "${speed}x"
                }
                DensePlayerActionChip(
                    label = label,
                    selected = selected,
                    onClick = { onSelectSpeed(speed) }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DensePlayerActionChip(
                label = if (autoPlayNextEnabled) "连播开" else "连播关",
                selected = autoPlayNextEnabled,
                onClick = onToggleAutoPlayNext
            )
            DensePlayerActionChip(
                label = if (gestureSeekEnabled) "手势开" else "手势关",
                selected = gestureSeekEnabled,
                onClick = onToggleGestureSeek
            )
        }
    }
}

@Composable
private fun DensePlayerActionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    Surface(
        modifier = modifier
            .wrapContentWidth()
            .clickable(onClickLabel = label, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun PlayerTransportControls(
    isPlaying: Boolean,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onIntent(PlayerContract.UiIntent.PlayPrevious) }) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = null, tint = Color.White)
        }
        IconButton(onClick = { onIntent(PlayerContract.UiIntent.SeekBackward) }) {
            Icon(Icons.Filled.Replay10, contentDescription = null, tint = Color.White)
        }
        FilledIconButton(onClick = { onIntent(PlayerContract.UiIntent.TogglePlayPause) }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null
            )
        }
        IconButton(onClick = { onIntent(PlayerContract.UiIntent.SeekForward) }) {
            Icon(Icons.Filled.Forward10, contentDescription = null, tint = Color.White)
        }
        IconButton(onClick = { onIntent(PlayerContract.UiIntent.PlayNext) }) {
            Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun PlayerTimeline(
    progress: PlayerContract.ProgressUi,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    val displayPosition = if (scrubbing) scrubPositionMs else progress.positionMs

    Column(modifier = modifier.padding(top = 8.dp)) {
        ScrubbableTimelineBar(
            currentPositionMs = displayPosition,
            durationMs = progress.durationMs,
            bufferedPositionMs = progress.bufferedPositionMs,
            onScrubStart = {
                scrubbing = true
                scrubPositionMs = progress.positionMs
                onIntent(PlayerContract.UiIntent.ScrubStarted)
            },
            onScrub = { positionMs ->
                scrubbing = true
                scrubPositionMs = positionMs
            },
            onScrubEnd = { positionMs ->
                scrubbing = false
                scrubPositionMs = positionMs
                onIntent(PlayerContract.UiIntent.SeekTo(positionMs))
                onIntent(PlayerContract.UiIntent.ScrubEnded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPosition),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = formatDuration(progress.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PlayerToastOverlay(
    message: String?,
    token: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrBlank()) return

    LaunchedEffect(token, message) {
        delay(2_200L)
        onDismiss()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.78f),
        contentColor = Color.White,
        shadowElevation = 6.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ResumePrompt(
    positionMs: Long,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.82f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "上次观看到 ${formatDuration(positionMs)}，是否继续？",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onRestart) {
                    Text("从头播放")
                }
                Button(onClick = onContinue) {
                    Text("继续播放")
                }
            }
        }
    }
}

@Composable
private fun ScrubbableTimelineBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = Color.White.copy(alpha = 0.24f)
    val bufferedColor = Color.White.copy(alpha = 0.42f)
    val playedColor = MaterialTheme.colorScheme.primary
    val handleColor = Color.White
    val clampedDuration = durationMs.coerceAtLeast(1L)
    val playedProgress = (currentPositionMs.toFloat() / clampedDuration.toFloat()).coerceIn(0f, 1f)
    val bufferedProgress = (bufferedPositionMs.toFloat() / clampedDuration.toFloat()).coerceIn(0f, 1f)

    val latestOnScrubStart by rememberUpdatedState(onScrubStart)
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestOnScrubEnd by rememberUpdatedState(onScrubEnd)

    Canvas(
        modifier = modifier.pointerInput(clampedDuration) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                fun positionForX(x: Float): Long {
                    val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                    return (fraction * clampedDuration).toLong()
                }
                latestOnScrubStart()
                var latest = positionForX(down.position.x)
                latestOnScrub(latest)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    latest = positionForX(change.position.x)
                    latestOnScrub(latest)
                    change.consume()
                    if (!change.pressed) {
                        latestOnScrubEnd(latest)
                        break
                    }
                }
            }
        }
    ) {
        val trackHeight = 4.dp.toPx()
        val activeTrackHeight = 5.dp.toPx()
        val cornerRadius = trackHeight / 2f
        val centerY = size.height / 2f
        val trackTop = centerY - trackHeight / 2f
        val playedWidth = size.width * playedProgress
        val bufferedWidth = size.width * bufferedProgress

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
        drawRoundRect(
            color = bufferedColor,
            topLeft = Offset(0f, trackTop),
            size = Size(bufferedWidth, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
        drawRoundRect(
            color = playedColor,
            topLeft = Offset(0f, centerY - activeTrackHeight / 2f),
            size = Size(playedWidth, activeTrackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                activeTrackHeight / 2f,
                activeTrackHeight / 2f
            )
        )
        drawCircle(
            color = handleColor,
            radius = 7.dp.toPx(),
            center = Offset(playedWidth.coerceIn(0f, size.width), centerY)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatPlayerTitle(videoTitle: String, episodeTitle: String): String {
    val normalizedVideoTitle = videoTitle.trim()
    val normalizedEpisodeTitle = episodeTitle.trim()
    return when {
        normalizedVideoTitle.isBlank() -> normalizedEpisodeTitle
        normalizedEpisodeTitle.isBlank() -> normalizedVideoTitle
        else -> "$normalizedVideoTitle($normalizedEpisodeTitle)"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412, heightDp = 280)
@Composable
private fun PlayerSurfaceSectionPreview() {
    val state = PlayerPreviewData.state()
    iCinemaTheme {
        PlayerSurfaceSection(
            chrome = state.toChromeUi(),
            progress = PlayerContract.ProgressUi(
                positionMs = state.currentPositionMs,
                durationMs = state.durationMs,
                bufferedPositionMs = state.bufferedPositionMs
            ),
            player = null,
            onBackClick = {},
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 915, heightDp = 412)
@Composable
private fun PlayerSurfaceSectionFullscreenPreview() {
    val state = PlayerPreviewData.state().copy(isFullscreen = true)
    iCinemaTheme {
        PlayerSurfaceSection(
            chrome = state.toChromeUi(),
            progress = PlayerContract.ProgressUi(
                positionMs = state.currentPositionMs,
                durationMs = state.durationMs,
                bufferedPositionMs = state.bufferedPositionMs
            ),
            player = null,
            onBackClick = {},
            onIntent = {}
        )
    }
}
