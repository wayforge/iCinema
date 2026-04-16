package com.icinema.pages.home

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

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

@Composable
private fun SearchInputCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearQuery: () -> Unit,
    isSearching: Boolean,
    focusManager: FocusManager
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            placeholder = {
                Text(
                    text = "搜索视频名称、演员、导演...",
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotBlank()) {
                        onSearch()
                        focusManager.clearFocus()
                    }
                }
            ),
            shape = RoundedCornerShape(24.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            ),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "清空",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = onSearch,
                        enabled = query.isNotBlank() && !isSearching,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "搜索",
                            color = if (query.isNotBlank() && !isSearching) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSuggestionSection(
    searchHistory: List<String>,
    hotKeywords: List<String>,
    onQuickSearch: (String) -> Unit,
    onClearSearchHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (searchHistory.isNotEmpty()) {
            SuggestionGroup(
                title = "搜索历史",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                action = {
                    TextButton(onClick = onClearSearchHistory) {
                        Text("清空")
                    }
                }
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searchHistory.forEach { keyword ->
                        AssistChip(
                            onClick = { onQuickSearch(keyword) },
                            label = { Text(keyword) }
                        )
                    }
                }
            }
        }

        if (hotKeywords.isNotEmpty()) {
            SuggestionGroup(
                title = "热门搜索",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Whatshot,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotKeywords.forEach { keyword ->
                        AssistChip(
                            onClick = { onQuickSearch(keyword) },
                            label = { Text(keyword) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Whatshot,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionGroup(
    title: String,
    icon: @Composable () -> Unit,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icon()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                action?.invoke()
            }
            content()
        }
    }
}
