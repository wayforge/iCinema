package com.icinema.pages.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.icinema.pages.home.components.HomeBottomNavigation
import com.icinema.pages.home.discover.DiscoverPage
import com.icinema.pages.home.mine.MinePage
import com.icinema.pages.home.search.SearchPage

@Composable
internal fun HomeShell(
    state: HomeContract.UiState,
    selectedTab: HomeTab,
    snackbarHostState: SnackbarHostState,
    onTabSelected: (HomeTab) -> Unit,
    onRetryDiscover: () -> Unit,
    onRetrySearch: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onRefreshDiscover: () -> Unit,
    onRefreshSearch: () -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onSearchInputChange: (String) -> Unit,
    onQuickSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenCategoryEditor: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorite: () -> Unit,
    onContinueWatchingClick: (Long, String, Int) -> Unit,
    onSortChange: (HomeContract.SortMode) -> Unit,
    onLoadMoreDiscover: () -> Unit,
    onLoadMoreSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            HomeBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (selectedTab) {
            HomeTab.Discover -> {
                DiscoverPage(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onRetry = onRetryDiscover,
                    onVideoClick = onVideoClick,
                    onRefresh = onRefreshDiscover,
                    onCategorySelected = onCategorySelected,
                    onOpenCategoryEditor = onOpenCategoryEditor,
                    onLoadMore = onLoadMoreDiscover,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            HomeTab.Search -> {
                SearchPage(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onRetry = onRetrySearch,
                    onSearch = onSearch,
                    onSearchInputChange = onSearchInputChange,
                    onQuickSearch = onQuickSearch,
                    onRefresh = onRefreshSearch,
                    onClearSearch = onClearSearch,
                    onClearSearchHistory = onClearSearchHistory,
                    onVideoClick = onVideoClick,
                    onLoadMore = onLoadMoreSearch,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            HomeTab.Mine -> {
                MinePage(
                    state = state,
                    onOpenHistory = onOpenHistory,
                    onOpenFavorite = onOpenFavorite,
                    onVideoClick = onVideoClick,
                    onContinueWatchingClick = onContinueWatchingClick,
                    onSortChange = onSortChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}
