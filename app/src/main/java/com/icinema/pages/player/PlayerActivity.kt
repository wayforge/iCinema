package com.icinema.pages.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icinema.ui.theme.iCinemaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_VIDEO_ID = "extra_video_id"
        private const val EXTRA_SOURCE_KEY = "extra_source_key"
        private const val EXTRA_EPISODE_INDEX = "extra_episode_index"

        fun start(
            context: Context,
            videoId: Long,
            sourceKey: String?,
            episodeIndex: Int
        ) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_SOURCE_KEY, sourceKey)
                putExtra(EXTRA_EPISODE_INDEX, episodeIndex)
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

        val sourceKey = intent.getStringExtra(EXTRA_SOURCE_KEY)
        val episodeIndex = intent.getIntExtra(EXTRA_EPISODE_INDEX, 0)

        setContent {
            iCinemaTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerScreen(
                        activity = this,
                        viewModel = viewModel<PlayerViewModel>(),
                        videoId = videoId,
                        sourceKey = sourceKey,
                        episodeIndex = episodeIndex,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    activity: PlayerActivity,
    viewModel: PlayerViewModel,
    videoId: Long,
    sourceKey: String?,
    episodeIndex: Int,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(videoId, sourceKey, episodeIndex) {
        viewModel.handleIntent(PlayerContract.UiIntent.Load(videoId, sourceKey, episodeIndex))
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PlayerContract.UiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(state.isFullscreen) {
        activity.requestedOrientation = if (state.isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (state.isFullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.handleIntent(PlayerContract.UiIntent.OnLifecycleStart)
                Lifecycle.Event.ON_STOP -> viewModel.handleIntent(PlayerContract.UiIntent.OnLifecycleStop)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        onBackClick()
    }

    PlayerContent(
        state = state,
        player = viewModel.player,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onIntent = viewModel::handleIntent
    )
}
