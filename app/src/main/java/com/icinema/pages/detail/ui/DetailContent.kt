package com.icinema.pages.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icinema.pages.detail.DetailContract
import com.icinema.pages.detail.preview.detailPreviewState
import com.icinema.pages.widgets.ErrorScreen
import com.icinema.pages.widgets.LoadingScreen
import com.icinema.ui.theme.iCinemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    state: DetailContract.UiState,
    onBackClick: () -> Unit,
    onIntent: (DetailContract.UiIntent) -> Unit,
    onOpenPlayer: (sourceKey: String?, episodeIndex: Int) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            if (state.video == null) {
                TopAppBar(
                    title = { Text("视频详情") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            state.isLoading -> {
                LoadingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.error != null -> {
                ErrorScreen(
                    message = state.error,
                    onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.video != null -> {
                DetailSuccessContent(
                    state = state,
                    onBackClick = onBackClick,
                    onIntent = onIntent,
                    onOpenPlayer = onOpenPlayer,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                ErrorScreen(
                    message = "详情暂不可用",
                    onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun DetailSuccessContent(
    state: DetailContract.UiState,
    onBackClick: () -> Unit,
    onIntent: (DetailContract.UiIntent) -> Unit,
    onOpenPlayer: (sourceKey: String?, episodeIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val video = state.video ?: return
    val playGroups = video.playGroups.filter { it.second.isNotEmpty() }
    val currentSource = state.selectedPlaySource ?: playGroups.firstOrNull()?.first
    val currentEpisodes = playGroups.firstOrNull { it.first == currentSource }?.second.orEmpty()
    val rangeSize = when {
        currentEpisodes.size > 100 -> 30
        currentEpisodes.size > 40 -> 20
        else -> 12
    }
    val totalRanges = if (currentEpisodes.isEmpty()) 0 else (currentEpisodes.size + rangeSize - 1) / rangeSize
    val clampedRange = state.selectedRange.coerceIn(0, (totalRanges - 1).coerceAtLeast(0))
    val startIndex = clampedRange * rangeSize
    val endIndex = minOf(startIndex + rangeSize, currentEpisodes.size)
    val rangeEpisodes = if (currentEpisodes.isEmpty()) emptyList() else currentEpisodes.subList(startIndex, endIndex)
    val selectedEpisode = currentEpisodes.getOrNull(state.selectedEpisode)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DetailHeroSection(
            video = video,
            currentSource = currentSource,
            selectedEpisode = selectedEpisode,
            episodeCount = currentEpisodes.size,
            isFavorite = state.isFavorite,
            onBackClick = onBackClick,
            onFavoriteClick = { onIntent(DetailContract.UiIntent.ToggleFavorite) },
            onPlayClick = {
                if (currentSource != null) {
                    onOpenPlayer(currentSource, state.selectedEpisode)
                }
            },
            modifier = Modifier.statusBarsPadding()
        )

        DetailPlaybackSection(
            playGroups = playGroups,
            currentSource = currentSource,
            currentEpisodes = currentEpisodes,
            totalRanges = totalRanges,
            rangeSize = rangeSize,
            clampedRange = clampedRange,
            rangeEpisodes = rangeEpisodes,
            startIndex = startIndex,
            selectedEpisode = state.selectedEpisode,
            onSelectPlaySource = { source ->
                onIntent(DetailContract.UiIntent.SelectPlaySource(source))
            },
            onSelectRange = { range ->
                onIntent(DetailContract.UiIntent.SelectRange(range))
            },
            onSelectEpisode = { episode ->
                onIntent(DetailContract.UiIntent.SelectEpisode(episode))
                onOpenPlayer(currentSource, episode)
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        DetailDescriptionSection(
            video = video,
            description = video.content,
            onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
            modifier = Modifier.padding(horizontal = 20.dp)
        )
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
