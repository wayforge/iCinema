package com.icinema.pages.home.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.icinema.pages.home.HomeContract
import com.icinema.pages.home.components.PageHeader
import com.icinema.pages.home.components.SimpleEmptyState
import com.icinema.pages.home.components.VideoGrid

@Composable
internal fun SearchPage(
    state: HomeContract.UiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchInputChange: (String) -> Unit,
    onQuickSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearSearch: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchState = state.searchState
    val searchResults = searchState.results
    var searchQuery by rememberSaveable { mutableStateOf(searchState.input) }
    val focusManager = LocalFocusManager.current
    val showSearchResults = searchState.hasSearched || searchState.isSearching ||
        (searchResults.error != null && searchState.query.isNotBlank())
    val showSuggestionSection = !showSearchResults && searchQuery.isBlank()

    LaunchedEffect(searchState.input) {
        if (searchState.input != searchQuery) {
            searchQuery = searchState.input
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(sectionTitle = "搜索")

        SearchInputCard(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                onSearchInputChange(it)
            },
            onSearch = {
                if (searchQuery.isNotBlank()) {
                    onSearch(searchQuery)
                    focusManager.clearFocus()
                }
            },
            onClearQuery = {
                searchQuery = ""
                onClearSearch()
            },
            isSearching = searchState.isSearching,
            focusManager = focusManager
        )

        if (showSuggestionSection) {
            SearchSuggestionSection(
                searchHistory = state.searchHistory.take(8),
                hotKeywords = state.hotKeywords.take(8),
                onQuickSearch = {
                    searchQuery = it
                    onQuickSearch(it)
                    focusManager.clearFocus()
                },
                onClearSearchHistory = onClearSearchHistory
            )
        }

        if (showSearchResults) {
            VideoGrid(
                videos = searchResults.videos,
                isLoading = searchResults.isLoading,
                isRefreshing = searchResults.isRefreshing || searchState.shouldShowRefreshIndicator,
                isLoadingMore = searchResults.isLoadingMore,
                error = searchResults.error,
                hasMorePages = searchResults.hasMorePages,
                snackbarHostState = snackbarHostState,
                onRetry = onRetry,
                onRefresh = onRefresh,
                onVideoClick = onVideoClick,
                onLoadMore = onLoadMore,
                emptyContent = {
                    SimpleEmptyState(
                        title = "没有找到相关内容",
                        subtitle = if (searchState.query.isBlank()) {
                            "换个关键词试试"
                        } else {
                            "“${searchState.query}” 暂无搜索结果"
                        }
                    )
                },
                modifier = Modifier.weight(1f),
                enablePullRefresh = true
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SimpleEmptyState(
                    title = "搜索影片、演员或导演",
                    subtitle = "输入关键词，或从历史与热词中快速开始"
                )
            }
        }
    }
}
