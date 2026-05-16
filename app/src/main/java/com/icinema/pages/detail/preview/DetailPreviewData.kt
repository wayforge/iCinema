package com.icinema.pages.detail.preview

import com.icinema.domain.model.Video
import com.icinema.pages.detail.DetailContract

fun detailPreviewState(): DetailContract.UiState {
    val sampleVideo = Video(
        id = 1L,
        name = "无尽夜航",
        pic = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800",
        picThumb = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400",
        actor = "顾遥, 周沉, 林见川",
        director = "许舟",
        content = "<p>这是一段用于详情页预览的样板简介，展示海报、元信息、播放源切换与选集浏览的完整布局。</p>",
        area = "中国",
        year = "2026",
        typeId = 1,
        typeName = "悬疑",
        playFrom = "main\$\$\$backup",
        playUrl = listOf(
            List(18) { index ->
                "第${index + 1}集\$https://example.com/main/${index + 1}.m3u8"
            }.joinToString("#"),
            List(6) { index ->
                "第${index + 1}集\$https://example.com/backup/${index + 1}.m3u8"
            }.joinToString("#")
        ).joinToString("\$\$\$"),
        total = 18
    )

    return DetailContract.UiState(
        currentVideoId = 1L,
        isLoading = false,
        video = sampleVideo,
        selectedPlaySource = "main",
        selectedEpisode = 2,
        selectedRange = 0
    )
}
