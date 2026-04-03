package com.icinema.pages.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icinema.pages.player.PlayerActivity
import com.icinema.ui.theme.iCinemaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_ID = "extra_video_id"

        fun start(context: Context, videoId: Long) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1L)
        if (videoId <= 0L) {
            finish()
            return
        }

        setContent {
            iCinemaTheme {
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.background
                ) {
                    DetailScreen(
                        context = this@DetailActivity,
                        viewModel = viewModel<DetailViewModel>(),
                        videoId = videoId,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(
    context: Context,
    viewModel: DetailViewModel,
    videoId: Long,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(videoId) {
        viewModel.handleIntent(DetailContract.UiIntent.LoadVideo(videoId))
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DetailContract.UiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    DisposableEffect(videoId) {
        onDispose {
            viewModel.handleIntent(DetailContract.UiIntent.ClearVideo)
        }
    }

    DetailContent(
        state = state,
        onBackClick = onBackClick,
        onIntent = viewModel::handleIntent,
        onOpenPlayer = { sourceKey, episodeIndex ->
            PlayerActivity.start(
                context = context,
                videoId = videoId,
                sourceKey = sourceKey,
                episodeIndex = episodeIndex
            )
        },
        snackbarHostState = snackbarHostState
    )
}
