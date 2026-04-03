package com.icinema.pages.player

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.icinema.domain.model.PlaySource
import com.icinema.domain.model.PlayableEpisode
import com.icinema.domain.model.Video
import com.icinema.ui.theme.iCinemaTheme

internal object PlayerPreviewData {
    val episodes = List(12) { index ->
        PlayableEpisode(
            index = index,
            title = "第${index + 1}集",
            url = "https://example.com/main/${index + 1}.m3u8",
            isHls = true
        )
    }

    val backupEpisodes = List(4) { index ->
        PlayableEpisode(
            index = index,
            title = "第${index + 1}集",
            url = "https://example.com/backup/${index + 1}.m3u8",
            isHls = true
        )
    }

    val playSources = listOf(
        PlaySource(key = "main", episodes = episodes),
        PlaySource(key = "backup", episodes = backupEpisodes)
    )

    fun state(): PlayerContract.UiState {
        return PlayerContract.UiState(
            videoId = 1L,
            video = Video(
                id = 1L,
                name = "无尽夜航",
                pic = "",
                picThumb = null,
                actor = "顾遥, 周沉, 林见川",
                director = "许舟",
                content = "这是一段用于播放器页面预览的样板简介，展示控制层、线路和选集入口。",
                area = "中国",
                year = "2026",
                typeId = 1,
                typeName = "悬疑",
                playFrom = null,
                playUrl = null,
                total = 12
            ),
            playSources = playSources,
            selectedSourceKey = "main",
            selectedEpisodeIndex = 2,
            currentEpisode = episodes[2],
            isLoading = false,
            isBuffering = false,
            isPlaying = true,
            currentPositionMs = 372_000L,
            durationMs = 2_640_000L,
            bufferedPositionMs = 690_000L,
            controlsVisible = true,
            canPlayNext = true,
            cacheEnabled = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412, heightDp = 915)
@Composable
private fun PlayerContentPreview() {
    iCinemaTheme {
        PlayerContent(
            state = PlayerPreviewData.state(),
            player = null,
            onBackClick = {},
            onIntent = {},
            snackbarHostState = SnackbarHostState()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 915, heightDp = 412)
@Composable
private fun PlayerContentFullscreenPreview() {
    iCinemaTheme {
        PlayerContent(
            state = PlayerPreviewData.state().copy(isFullscreen = true),
            player = null,
            onBackClick = {},
            onIntent = {},
            snackbarHostState = SnackbarHostState()
        )
    }
}
