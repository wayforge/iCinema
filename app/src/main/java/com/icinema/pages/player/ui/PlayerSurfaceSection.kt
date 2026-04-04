package com.icinema.pages.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    Box(
        modifier = modifier
            .background(Color.Black)
            .then(
                if (state.isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }
            )
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
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
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
            Button(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("重试")
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
            title = state.video?.name.orEmpty(),
            isFullscreen = state.isFullscreen,
            onBackClick = onBackClick,
            onIntent = onIntent
        )

        Spacer(modifier = Modifier.weight(1f))

        PlayerTransportControls(
            isPlaying = state.isPlaying,
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
    isFullscreen: Boolean,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = {
                onIntent(
                    if (isFullscreen) PlayerContract.UiIntent.ExitFullscreen
                    else PlayerContract.UiIntent.EnterFullscreen
                )
            }
        ) {
            Icon(
                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = null,
                tint = Color.White
            )
        }
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
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Slider(
            value = currentPositionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        LinearProgressIndicator(
            progress = {
                if (durationMs <= 0L) 0f else bufferedPositionMs.toFloat() / durationMs.toFloat()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
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

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
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
