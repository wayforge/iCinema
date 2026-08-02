package com.icinema.pages.player.adfilter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.icinema.pages.player.core.PlaybackMediaSourceFactory
import com.icinema.ui.theme.iCinemaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HlsAdClipPreviewActivity : ComponentActivity() {
    @Inject lateinit var playbackMediaSourceFactory: PlaybackMediaSourceFactory

    companion object {
        private const val EXTRA_SEGMENT_URL = "extra_segment_url"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, segmentUrl: String, title: String) {
            context.startActivity(
                Intent(context, HlsAdClipPreviewActivity::class.java).apply {
                    putExtra(EXTRA_SEGMENT_URL, segmentUrl)
                    putExtra(EXTRA_TITLE, title)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val segmentUrl = intent.getStringExtra(EXTRA_SEGMENT_URL).orEmpty()
        if (segmentUrl.isBlank()) {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "广告片段" }

        setContent {
            iCinemaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HlsAdClipPreviewScreen(
                        title = title,
                        segmentUrl = segmentUrl,
                        playbackMediaSourceFactory = playbackMediaSourceFactory,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HlsAdClipPreviewScreen(
    title: String,
    segmentUrl: String,
    playbackMediaSourceFactory: PlaybackMediaSourceFactory,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(segmentUrl) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(playbackMediaSourceFactory.createMediaSourceFactory())
            .build()
            .apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(segmentUrl)
                        .setMimeType(MimeTypes.VIDEO_MP2T)
                        .build()
                )
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = true
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        )
    }
}
