package com.icinema.pages.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.icinema.ui.theme.iCinemaTheme

@Composable
internal fun PlayerSurfaceSection(
    state: PlayerContract.UiState,
    player: ExoPlayer?,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    var showErrorDetails by remember(state.errorDetail) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .fillMaxSize()
            .pointerInput(state.gestureSeekEnabled) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (state.gestureSeekEnabled && !state.controlsLocked) {
                        val delta = (dragAmount * 180L).toLong()
                        onIntent(PlayerContract.UiIntent.GestureSeek(delta))
                    }
                }
            }
            .clickable { onIntent(PlayerContract.UiIntent.ToggleControls) }
    ) {
        PlayerRuntimeSurface(
            player = player,
            isPreview = isPreview,
            modifier = Modifier.fillMaxSize()
        )

        if (state.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        if (state.controlsVisible) {
            PlayerControlsOverlay(
                state = state,
                onBackClick = onBackClick,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (state.resumePositionMs != null) {
            ResumePrompt(
                positionMs = state.resumePositionMs,
                onContinue = { onIntent(PlayerContract.UiIntent.AcceptResume) },
                onRestart = { onIntent(PlayerContract.UiIntent.RestartFromBeginning) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        if (!state.isLoading && state.error != null) {
            PlayerErrorCard(
                message = state.error,
                onRetry = { onIntent(PlayerContract.UiIntent.Retry) },
                onShowDetails = state.errorDetail?.let { { showErrorDetails = true } },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }

        if (showErrorDetails && state.errorDetail != null) {
            AlertDialog(
                onDismissRequest = { showErrorDetails = false },
                title = { Text("播放错误详情") },
                text = {
                    Text(
                        text = state.errorDetail,
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
            PlayerView(context).apply {
                useController = false
                this.player = player
            }
        },
        update = { it.player = player },
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
private fun PlayerControlsOverlay(
    state: PlayerContract.UiState,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.48f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.56f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        PlayerTopBar(
            title = formatPlayerTitle(
                videoTitle = state.video?.name.orEmpty(),
                episodeTitle = state.currentEpisode?.title.orEmpty()
            ),
            isLocked = state.controlsLocked,
            playbackSpeed = state.playbackSpeed,
            autoPlayNextEnabled = state.autoPlayNextEnabled,
            gestureSeekEnabled = state.gestureSeekEnabled,
            onBackClick = onBackClick,
            onIntent = onIntent
        )

        Spacer(modifier = Modifier.weight(1f))

        PlayerTransportControls(
            isPlaying = state.isPlaying,
            isLocked = state.controlsLocked,
            onIntent = onIntent
        )

        PlayerTimeline(
            currentPositionMs = state.currentPositionMs,
            durationMs = state.durationMs,
            bufferedPositionMs = state.bufferedPositionMs,
            onSeek = { onIntent(PlayerContract.UiIntent.SeekTo(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    isLocked: Boolean,
    playbackSpeed: Float,
    autoPlayNextEnabled: Boolean,
    gestureSeekEnabled: Boolean,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    var showSettingsMenu by remember { mutableStateOf(false) }

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
        IconButton(onClick = { onIntent(PlayerContract.UiIntent.ToggleControlsLock) }) {
            Icon(
                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = "锁定",
                tint = Color.White
            )
        }
        if (!isLocked) {
            Box {
                IconButton(onClick = { showSettingsMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "播放设置",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false }
                ) {
                    PlaybackSpeedMenuItem(
                        label = "1.0x",
                        selected = playbackSpeed == 1.0f,
                        onClick = {
                            onIntent(PlayerContract.UiIntent.SetPlaybackSpeed(1.0f))
                            showSettingsMenu = false
                        }
                    )
                    PlaybackSpeedMenuItem(
                        label = "1.25x",
                        selected = playbackSpeed == 1.25f,
                        onClick = {
                            onIntent(PlayerContract.UiIntent.SetPlaybackSpeed(1.25f))
                            showSettingsMenu = false
                        }
                    )
                    PlaybackSpeedMenuItem(
                        label = "1.5x",
                        selected = playbackSpeed == 1.5f,
                        onClick = {
                            onIntent(PlayerContract.UiIntent.SetPlaybackSpeed(1.5f))
                            showSettingsMenu = false
                        }
                    )
                    PlaybackSpeedMenuItem(
                        label = "2.0x",
                        selected = playbackSpeed == 2.0f,
                        onClick = {
                            onIntent(PlayerContract.UiIntent.SetPlaybackSpeed(2.0f))
                            showSettingsMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (autoPlayNextEnabled) "连播：开" else "连播：关") },
                        onClick = { onIntent(PlayerContract.UiIntent.ToggleAutoPlayNext) },
                        trailingIcon = {
                            if (autoPlayNextEnabled) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (gestureSeekEnabled) "手势：开" else "手势：关") },
                        onClick = { onIntent(PlayerContract.UiIntent.ToggleGestureSeek) },
                        trailingIcon = {
                            if (gestureSeekEnabled) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
            IconButton(onClick = { onIntent(PlayerContract.UiIntent.MarkCurrentSegmentAsAd) }) {
                Icon(
                    imageVector = Icons.Filled.Report,
                    contentDescription = "标记广告",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlaybackSpeedMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("倍速：$label") },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null)
            }
        }
    )
}

@Composable
private fun PlayerTransportControls(
    isPlaying: Boolean,
    isLocked: Boolean,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLocked) {
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
        } else {
            FilledIconButton(onClick = { onIntent(PlayerContract.UiIntent.TogglePlayPause) }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun PlayerTimeline(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        ScrubbableTimelineBar(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPositionMs,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPositionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = formatDuration(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "继续播放到 ${formatDuration(positionMs)}",
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onContinue) {
                    Text("继续播放")
                }
                TextButton(onClick = onRestart) {
                    Text("从头播放")
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
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = Color.White.copy(alpha = 0.24f)
    val bufferedColor = Color.White.copy(alpha = 0.42f)
    val playedColor = MaterialTheme.colorScheme.primary
    val handleColor = Color.White
    val clampedDuration = durationMs.coerceAtLeast(1L)
    val playedProgress = (currentPositionMs.toFloat() / clampedDuration.toFloat()).coerceIn(0f, 1f)
    val bufferedProgress = (bufferedPositionMs.toFloat() / clampedDuration.toFloat()).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier.pointerInput(clampedDuration) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent().changes.firstOrNull { it.changedToDown() } ?: continue
                    fun seekToX(x: Float) {
                        val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((fraction * clampedDuration).toLong())
                    }

                    seekToX(down.position.x)
                    down.consumeAllChanges()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.positionChange() != Offset.Zero) {
                            seekToX(change.position.x)
                            change.consumeAllChanges()
                        }
                        if (!change.pressed) break
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
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(activeTrackHeight / 2f, activeTrackHeight / 2f)
        )
        drawCircle(
            color = handleColor,
            radius = 6.dp.toPx(),
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
    iCinemaTheme {
        PlayerSurfaceSection(
            state = PlayerPreviewData.state(),
            player = null,
            onBackClick = {},
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 915, heightDp = 412)
@Composable
private fun PlayerSurfaceSectionFullscreenPreview() {
    iCinemaTheme {
        PlayerSurfaceSection(
            state = PlayerPreviewData.state().copy(isFullscreen = true),
            player = null,
            onBackClick = {},
            onIntent = {}
        )
    }
}
