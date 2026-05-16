package com.icinema.pages.home.preview

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.icinema.pages.home.HomeShell
import com.icinema.pages.home.HomeTab
import com.icinema.ui.theme.iCinemaTheme

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 412, heightDp = 915)
@Composable
private fun HomeDiscoverPreview() {
    HomeShellPreview(selectedTab = HomeTab.Discover)
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 412, heightDp = 915)
@Composable
private fun HomeSearchPreview() {
    HomeShellPreview(selectedTab = HomeTab.Search)
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 412, heightDp = 915)
@Composable
private fun HomeMinePreview() {
    HomeShellPreview(selectedTab = HomeTab.Mine)
}

@Composable
private fun HomeShellPreview(selectedTab: HomeTab) {
    iCinemaTheme {
        HomeShell(
            state = HomePreviewData.uiState,
            selectedTab = selectedTab,
            snackbarHostState = remember { SnackbarHostState() },
            onTabSelected = {},
            onRetryDiscover = {},
            onRetrySearch = {},
            onVideoClick = {},
            onRefreshDiscover = {},
            onRefreshSearch = {},
            onCategorySelected = {},
            onSearch = {},
            onSearchInputChange = {},
            onQuickSearch = {},
            onClearSearch = {},
            onClearSearchHistory = {},
            onOpenCategoryEditor = {},
            onOpenHistory = {},
            onOpenFavorite = {},
            onContinueWatchingClick = { _, _, _ -> },
            onSortChange = {},
            onLoadMoreDiscover = {},
            onLoadMoreSearch = {}
        )
    }
}
