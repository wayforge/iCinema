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
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContent(
    state: PlayerContract.UiState,
    chrome: PlayerChromeUi,
    progress: PlayerContract.ProgressUi,
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
        onOpenEpisodes = { onIntent(PlayerContract.UiIntent.OpenSheet(PlayerContract.SheetMode.Episodes)) },
        onRefreshCastDevices = { onIntent(PlayerContract.UiIntent.RefreshCastDevices) },
        onSelectCastDevice = { deviceId -> onIntent(PlayerContract.UiIntent.SelectCastDevice(deviceId)) },
        onStopCasting = { onIntent(PlayerContract.UiIntent.StopCasting) }
    )

    AutoAcceptResumePrompt(
        resumePositionMs = chrome.resumePositionMs,
        onIntent = onIntent
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        paddingValues
        if (chrome.isLoading && chrome.video == null) {
            PlayerLoadingState(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize()) {
            PlayerSurfaceSection(
                chrome = chrome,
                progress = progress,
                player = player,
                onBackClick = onBackClick,
                onIntent = onIntent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AutoAcceptResumePrompt(
    resumePositionMs: Long?,
    onIntent: (PlayerContract.UiIntent) -> Unit
) {
    LaunchedEffect(resumePositionMs) {
        if (resumePositionMs != null) {
            delay(5_000L)
            onIntent(PlayerContract.UiIntent.AcceptResume)
        }
    }
}
