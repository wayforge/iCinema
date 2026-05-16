package com.icinema.pages.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    onVideoClick: (Long) -> Unit,
    onContinueWatchingClick: (Long, String, Int) -> Unit,
    onOpenCategoryEditor: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorite: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.Discover.name) }
    val selectedTab = remember(selectedTabName) { HomeTab.valueOf(selectedTabName) }

    HomeShell(
        state = state,
        selectedTab = selectedTab,
        snackbarHostState = snackbarHostState,
        onTabSelected = { tab ->
            if (tab != selectedTab) {
                selectedTabName = tab.name
                when (tab) {
                    HomeTab.Discover -> viewModel.handleIntent(HomeContract.UiIntent.RestoreDiscover)
                    HomeTab.Search -> viewModel.handleIntent(HomeContract.UiIntent.RestoreSearch)
                    HomeTab.Mine -> viewModel.refreshMineTab()
                }
            }
        },
        onRetryDiscover = { viewModel.handleIntent(HomeContract.UiIntent.RefreshDiscover) },
        onRetrySearch = {
            val query = state.searchState.query
            if (query.isNotBlank()) {
                viewModel.handleIntent(HomeContract.UiIntent.Search(query))
            }
        },
        onVideoClick = onVideoClick,
        onRefreshDiscover = { viewModel.handleIntent(HomeContract.UiIntent.RefreshDiscover) },
        onRefreshSearch = { viewModel.handleIntent(HomeContract.UiIntent.RefreshSearch) },
        onCategorySelected = { categoryId ->
            viewModel.handleIntent(HomeContract.UiIntent.SelectCategory(categoryId))
        },
        onSearch = { keyword ->
            viewModel.handleIntent(HomeContract.UiIntent.Search(keyword))
        },
        onSearchInputChange = { input ->
            viewModel.updateSearchInput(input)
        },
        onQuickSearch = { keyword ->
            viewModel.handleIntent(HomeContract.UiIntent.QuickSearch(keyword))
        },
        onClearSearch = {
            viewModel.handleIntent(HomeContract.UiIntent.ClearSearch)
        },
        onClearSearchHistory = {
            viewModel.handleIntent(HomeContract.UiIntent.ClearSearchHistory)
        },
        onOpenCategoryEditor = onOpenCategoryEditor,
        onOpenHistory = onOpenHistory,
        onOpenFavorite = onOpenFavorite,
        onContinueWatchingClick = onContinueWatchingClick,
        onSortChange = { sortMode ->
            viewModel.handleIntent(HomeContract.UiIntent.ChangeSort(sortMode))
        },
        onLoadMoreDiscover = { viewModel.handleIntent(HomeContract.UiIntent.LoadMoreDiscover) },
        onLoadMoreSearch = { viewModel.handleIntent(HomeContract.UiIntent.LoadMoreSearch) }
    )
}
