package com.icinema.pages.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.icinema.domain.model.PlaySource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContent(
    state: PlayerContract.UiState,
    player: ExoPlayer?,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val selectedSource = state.playSources.firstOrNull { it.key == state.selectedSourceKey }

    PlayerSheetHost(
        sheetMode = state.activeSheetMode,
        state = state,
        selectedSource = selectedSource,
        onDismiss = { onIntent(PlayerContract.UiIntent.DismissSheet) },
        onSelectSource = { sourceKey ->
            onIntent(PlayerContract.UiIntent.SelectSource(sourceKey))
        },
        onSelectEpisode = { episodeIndex ->
            onIntent(PlayerContract.UiIntent.SelectEpisode(episodeIndex))
        },
        onOpenSources = { onIntent(PlayerContract.UiIntent.OpenSheet(PlayerContract.SheetMode.Sources)) },
        onOpenEpisodes = { onIntent(PlayerContract.UiIntent.OpenSheet(PlayerContract.SheetMode.Episodes)) }
    )

    AutoDismissPlayerControls(state = state, onIntent = onIntent)
    AutoAcceptResumePrompt(state = state, onIntent = onIntent)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (state.isLoading && state.video == null) {
            PlayerLoadingState(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        PlayerPage(
            state = state,
            player = player,
            selectedSource = selectedSource,
            onBackClick = onBackClick,
            onIntent = onIntent,
            onOpenSources = { onIntent(PlayerContract.UiIntent.OpenSheet(PlayerContract.SheetMode.Sources)) },
            onOpenEpisodes = { onIntent(PlayerContract.UiIntent.OpenSheet(PlayerContract.SheetMode.Episodes)) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun AutoDismissPlayerControls(
    state: PlayerContract.UiState,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    LaunchedEffect(state.controlsVisible, state.isPlaying, state.activeSheetMode) {
        if (state.controlsVisible && state.isPlaying && state.activeSheetMode == null) {
            delay(3_000L)
            onIntent(PlayerContract.UiIntent.ToggleControls)
        }
    }
}

@Composable
private fun AutoAcceptResumePrompt(
    state: PlayerContract.UiState,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    LaunchedEffect(state.resumePositionMs) {
        if (state.resumePositionMs != null) {
            delay(5_000L)
            onIntent(PlayerContract.UiIntent.AcceptResume)
        }
    }
}

@Composable
private fun PlayerPage(
    state: PlayerContract.UiState,
    player: ExoPlayer?,
    selectedSource: PlaySource?,
    onBackClick: () -> Unit,
    onIntent: (PlayerContract.UiIntent) -> Unit,
    onOpenSources: () -> Unit,
    onOpenEpisodes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        PlayerSurfaceSection(
            state = state,
            player = player,
            onBackClick = onBackClick,
            onIntent = onIntent,
            modifier = Modifier
        )
    }
}
