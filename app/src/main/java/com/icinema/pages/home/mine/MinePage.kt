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
        ContinueWatchingSection(
            items = state.continueWatching,
            onContinueWatchingClick = onContinueWatchingClick
        )

        RecommendationSection(
            videos = state.recommendedVideos,
            historyCount = state.historyCount,
            recommendationCount = state.recommendedVideos.size,
            sortMode = state.sortMode,
            onOpenHistory = onOpenHistory,
            onOpenFavorite = onOpenFavorite,
            onVideoClick = onVideoClick,
            onSortChange = onSortChange
        )
    }
}
