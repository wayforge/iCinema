package com.icinema.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

private enum class HomeTab(val label: String) {
    Discover("发现"),
    Search("搜索"),
    Mine("我的")
}

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
    val state by viewModel.uiState.collectAsState()
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

@Composable
private fun HomeShell(
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
    onLoadMoreSearch: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
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

@Composable
private fun BottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
