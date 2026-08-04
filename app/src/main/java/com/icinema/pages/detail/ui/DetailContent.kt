package com.icinema.pages.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icinema.cast.ui.CastDeviceSheetContent
import com.icinema.cast.ui.CastMiniController
import com.icinema.pages.detail.DetailContract
import com.icinema.pages.detail.preview.detailPreviewState
import com.icinema.pages.widgets.ErrorScreen
import com.icinema.pages.widgets.LoadingScreen
import com.icinema.ui.theme.iCinemaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    state: DetailContract.UiState,
    onBackClick: () -> Unit,
    onIntent: (DetailContract.UiIntent) -> Unit,
    onOpenPlayer: (sourceKey: String?, episodeIndex: Int) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.video?.name ?: "视频详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.video != null) {
                        IconButton(
                            onClick = { onIntent(DetailContract.UiIntent.ToggleFavorite) }
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = if (state.isFavorite) "取消收藏" else "收藏",
                                tint = if (state.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
                }

                state.error != null -> {
                    ErrorScreen(
                        message = state.error,
                        onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                state.video != null -> {
                    DetailSuccessContent(
                        state = state,
                        onIntent = onIntent,
                        onCopyEpisodeLink = { label, url ->
                            clipboardManager.setText(AnnotatedString(url))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已复制 $label 链接")
                            }
                        },
                        onOpenPlayer = onOpenPlayer,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    ErrorScreen(
                        message = "详情暂不可用",
                        onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (state.castState.isCasting || state.castState.isConnecting) {
                CastMiniController(
                    state = state.castState,
                    onTogglePlayPause = { onIntent(DetailContract.UiIntent.ToggleCastPlayPause) },
                    onStopCasting = { onIntent(DetailContract.UiIntent.StopCasting) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                )
            }
        }

        if (state.isCastSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onIntent(DetailContract.UiIntent.DismissCastFlow) }
            ) {
                CastDeviceSheetContent(
                    state = state.castState,
                    onRefresh = { onIntent(DetailContract.UiIntent.RefreshCastDevices) },
                    onSelectDevice = { deviceId ->
                        onIntent(DetailContract.UiIntent.SelectCastDevice(deviceId))
                    },
                    onStopCasting = { onIntent(DetailContract.UiIntent.StopCasting) }
                )
            }
        }
    }
}

@Composable
private fun DetailSuccessContent(
    state: DetailContract.UiState,
    onIntent: (DetailContract.UiIntent) -> Unit,
    onCopyEpisodeLink: (String, String) -> Unit,
    onOpenPlayer: (sourceKey: String?, episodeIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val video = state.video ?: return
    val playGroups = video.playGroups.filter { it.second.isNotEmpty() }
    val currentSource = state.selectedPlaySource ?: playGroups.firstOrNull()?.first
    val currentEpisodes = playGroups.firstOrNull { it.first == currentSource }?.second.orEmpty()
    val selectedEpisode = currentEpisodes.getOrNull(state.selectedEpisode)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item(key = "hero") {
            DetailHeroSection(
                video = video,
                playGroups = playGroups,
                currentSource = currentSource,
                selectedEpisode = selectedEpisode,
                episodeCount = currentEpisodes.size,
                onSelectPlaySource = { source ->
                    onIntent(DetailContract.UiIntent.SelectPlaySource(source))
                },
                onOpenCurrentEpisode = {
                    if (currentSource != null) {
                        onOpenPlayer(currentSource, state.selectedEpisode)
                    }
                },
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
        }

        item(key = "playback") {
            DetailPlaybackSection(
                currentSource = currentSource,
                currentEpisodes = currentEpisodes,
                selectedRange = state.selectedRange,
                selectedEpisode = state.selectedEpisode,
                onSelectRange = { range ->
                    onIntent(DetailContract.UiIntent.SelectRange(range))
                },
                onSelectEpisode = { episode ->
                    onIntent(DetailContract.UiIntent.SelectEpisode(episode))
                    onOpenPlayer(currentSource, episode)
                },
                onCopyEpisodeLink = onCopyEpisodeLink,
                onCastEpisode = { episode ->
                    currentSource?.let { source ->
                        onIntent(DetailContract.UiIntent.OpenCastFlow(source, episode))
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun DetailContentPreview() {
    iCinemaTheme {
        DetailContent(
            state = detailPreviewState(),
            onBackClick = {},
            onIntent = {},
            onOpenPlayer = { _, _ -> }
        )
    }
}
