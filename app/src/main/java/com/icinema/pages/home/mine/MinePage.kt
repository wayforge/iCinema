package com.icinema.pages.home.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icinema.pages.home.HomeContract
import com.icinema.pages.home.HomeContract.SortMode
import com.icinema.pages.home.components.PageHeader

@Composable
internal fun MinePage(
    state: HomeContract.UiState,
    onOpenHistory: () -> Unit,
    onOpenFavorite: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onContinueWatchingClick: (Long, String, Int) -> Unit,
    onSortChange: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader(sectionTitle = "我的")

        MineHeroCard(
            historyCount = state.historyCount,
            continueWatchingCount = state.continueWatching.size,
            recommendationCount = state.recommendedVideos.size
        )

        QuickEntryRow(
            historyCount = state.historyCount,
            onOpenHistory = onOpenHistory,
            onOpenFavorite = onOpenFavorite
        )

        ContinueWatchingSection(
            items = state.continueWatching,
            onContinueWatchingClick = onContinueWatchingClick
        )

        if (state.recommendedVideos.isNotEmpty()) {
            RecommendationSection(
                videos = state.recommendedVideos,
                sortMode = state.sortMode,
                onVideoClick = onVideoClick,
                onSortChange = onSortChange
            )
        } else {
            EmptySectionCard(
                title = "暂无猜你喜欢内容",
                subtitle = "先去看看历史或收藏，系统会逐步为你整理推荐"
            )
        }
    }
}
