package com.icinema.pages.home.preview

import com.icinema.domain.model.Category
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import com.icinema.pages.home.HomeContract

internal object HomePreviewData {
    val categories = listOf(
        Category(id = 1, name = "电影", parentId = null),
        Category(id = 2, name = "连续剧", parentId = null),
        Category(id = 3, name = "动漫", parentId = null),
        Category(id = 4, name = "纪录片", parentId = null)
    )

    val videos = listOf(
        video(id = 1001, name = "午夜放映厅", typeName = "电影"),
        video(id = 1002, name = "城市追光者", typeName = "连续剧"),
        video(id = 1003, name = "海岸线计划", typeName = "纪录片"),
        video(id = 1004, name = "银河便利店", typeName = "动漫"),
        video(id = 1005, name = "长夜未央", typeName = "电影"),
        video(id = 1006, name = "风暴眼", typeName = "连续剧")
    )

    val uiState = HomeContract.UiState(
        discoverState = HomeContract.VideoSectionState(
            videos = videos,
            hasMorePages = true
        ),
        searchState = HomeContract.SearchSectionState(
            input = "城市",
            query = "城市",
            hasSearched = true,
            results = HomeContract.VideoSectionState(
                videos = videos.take(3),
                hasMorePages = false
            )
        ),
        categories = categories,
        visibleCategories = categories,
        selectedCategoryId = null,
        selectedCategoryIds = categories.map { it.id }.toSet(),
        continueWatching = listOf(
            WatchHistoryItem(
                id = 1,
                videoId = 1002,
                videoName = "城市追光者",
                videoPic = "",
                sourceKey = "preview",
                episodeIndex = 4,
                episodeTitle = "第5集 城市灯火",
                positionMs = 18 * 60 * 1000L,
                durationMs = 45 * 60 * 1000L,
                updatedAt = 0L,
                completed = false
            )
        ),
        historyCount = 18,
        searchHistory = listOf("城市", "科幻", "纪录片"),
        hotKeywords = listOf("影院", "追光者", "风暴", "银河"),
        recommendedVideos = videos.take(4),
        sortMode = HomeContract.SortMode.Latest
    )

    private fun video(
        id: Long,
        name: String,
        typeName: String
    ) = Video(
        id = id,
        name = name,
        pic = "",
        picThumb = null,
        actor = "预览演员",
        director = "预览导演",
        content = "用于 Compose Preview 的页面展示数据。",
        area = "内地",
        year = "2026",
        typeId = null,
        typeName = typeName,
        playFrom = null,
        playUrl = null,
        total = null
    )
}
