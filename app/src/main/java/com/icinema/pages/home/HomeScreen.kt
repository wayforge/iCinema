package com.icinema.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icinema.domain.model.Category
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import com.icinema.pages.home.HomeContract.SortMode
import com.icinema.pages.widgets.EmptyState
import com.icinema.pages.widgets.ErrorScreen
import com.icinema.pages.widgets.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = { viewModel.handleIntent(HomeContract.UiIntent.Refresh) },
        onVideoClick = onVideoClick,
        onContinueWatchingClick = onContinueWatchingClick,
        onRefresh = { viewModel.handleIntent(HomeContract.UiIntent.Refresh) },
        onCategorySelected = { categoryId ->
            viewModel.handleIntent(HomeContract.UiIntent.SelectCategory(categoryId))
        },
        onSearch = { keyword ->
            viewModel.handleIntent(HomeContract.UiIntent.Search(keyword))
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
        onSortChange = { sortMode ->
            viewModel.handleIntent(HomeContract.UiIntent.ChangeSort(sortMode))
        },
        onLoadMore = { viewModel.handleIntent(HomeContract.UiIntent.LoadMore) }
    )
}

@Composable
private fun ContinueWatchingSection(
    items: List<WatchHistoryItem>,
    onContinueWatchingClick: (Long, String, Int) -> Unit,
    onOpenHistory: () -> Unit
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "继续观看",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onOpenHistory) {
                Text("全部历史")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.width(220.dp),
                    onClick = {
                        onContinueWatchingClick(item.videoId, item.sourceKey, item.episodeIndex)
                    }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = item.videoName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.episodeTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationSection(
    videos: List<Video>,
    sortMode: SortMode,
    onVideoClick: (Long) -> Unit,
    onSortChange: (SortMode) -> Unit
) {
    if (videos.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("猜你喜欢", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = sortMode == SortMode.Latest,
                    onClick = { onSortChange(SortMode.Latest) },
                    label = { Text("最新") }
                )
                FilterChip(
                    selected = sortMode == SortMode.Name,
                    onClick = { onSortChange(SortMode.Name) },
                    label = { Text("名称") }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(videos, key = { it.id }) { video ->
                Card(
                    modifier = Modifier.width(220.dp),
                    onClick = { onVideoClick(video.id) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = video.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = video.typeName.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text("全部") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            ) {
                if (video.pic.isNotEmpty()) {
                    AsyncImage(
                        model = video.pic,
                        contentDescription = video.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "播放",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                video.typeName?.let { type ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    state: HomeContract.UiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onContinueWatchingClick: (Long, String, Int) -> Unit,
    onRefresh: () -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onQuickSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenCategoryEditor: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorite: () -> Unit,
    onSortChange: (SortMode) -> Unit,
    onLoadMore: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(state.searchKeyword) }
    var isSearchActive by remember { mutableStateOf(state.isSearchMode) }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh
    )

    LaunchedEffect(state.searchKeyword) {
        if (state.searchKeyword != searchQuery) {
            searchQuery = state.searchKeyword
        }
    }

    LaunchedEffect(state.isSearchMode) {
        isSearchActive = state.isSearchMode
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!state.isSearchMode) Modifier.pullRefresh(pullRefreshState) else Modifier),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AnimatedVisibility(
                                            visible = state.isSearchMode,
                                            enter = fadeIn() + slideInHorizontally(),
                                            exit = fadeOut() + slideOutHorizontally()
                                        ) {
                                            IconButton(onClick = {
                                                onClearSearch()
                                                isSearchActive = false
                                                searchQuery = ""
                                                focusManager.clearFocus()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "返回列表",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        AnimatedContent(
                                            targetState = state.isSearchMode,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "home_header_title"
                                        ) { searchMode ->
                                            Text(
                                                text = if (searchMode) "搜索: ${state.searchKeyword}" else "iCinema",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!state.isSearchMode) {
                                            TextButton(onClick = onOpenFavorite) { Text("收藏") }
                                            TextButton(onClick = onOpenHistory) { Text("历史") }
                                            TextButton(onClick = onOpenCategoryEditor) { Text("编辑分类") }
                                        }
                                        IconButton(onClick = {
                                            if (isSearchActive) {
                                                isSearchActive = false
                                            } else {
                                                searchQuery = ""
                                                isSearchActive = true
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "搜索",
                                                tint = if (isSearchActive) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isSearchActive,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("搜索视频名称、演员、导演...") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    if (searchQuery.isNotBlank()) {
                                                        onSearch(searchQuery)
                                                        focusManager.clearFocus()
                                                    }
                                                }
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                                cursorColor = MaterialTheme.colorScheme.primary
                                            ),
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (state.isSearching) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .padding(end = 8.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(
                                                                Icons.Default.Clear,
                                                                contentDescription = "清空",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            if (searchQuery.isNotBlank()) {
                                                                onSearch(searchQuery)
                                                                focusManager.clearFocus()
                                                            }
                                                        },
                                                        enabled = searchQuery.isNotBlank() && !state.isSearching
                                                    ) {
                                                        Text(
                                                            "搜索",
                                                            color = if (searchQuery.isNotBlank() && !state.isSearching) {
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

                                        if (!state.isSearchMode) {
                                            if (state.searchHistory.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("搜索历史", style = MaterialTheme.typography.labelMedium)
                                                    TextButton(onClick = onClearSearchHistory) { Text("清空") }
                                                }
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    state.searchHistory.forEach { keyword ->
                                                        FilterChip(
                                                            selected = false,
                                                            onClick = { onQuickSearch(keyword) },
                                                            label = { Text(keyword) }
                                                        )
                                                    }
                                                }
                                            }

                                            if (state.hotKeywords.isNotEmpty()) {
                                                Text(
                                                    text = "热词",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                                )
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    state.hotKeywords.forEach { keyword ->
                                                        AssistChip(
                                                            onClick = { onQuickSearch(keyword) },
                                                            label = { Text(keyword) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !isSearchActive,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ContinueWatchingSection(
                                    items = state.continueWatching,
                                    onContinueWatchingClick = onContinueWatchingClick,
                                    onOpenHistory = onOpenHistory
                                )
                                RecommendationSection(
                                    videos = state.recommendedVideos,
                                    sortMode = state.sortMode,
                                    onVideoClick = onVideoClick,
                                    onSortChange = onSortChange
                                )
                                if (state.visibleCategories.isNotEmpty()) {
                                    CategoryTabs(
                                        categories = state.visibleCategories,
                                        selectedCategoryId = state.selectedCategoryId,
                                        onCategorySelected = onCategorySelected
                                    )
                                }
                            }
                        }
                    }
                }

                when {
                    state.isLoading && state.videos.isEmpty() -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadingScreen(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp)
                            )
                        }
                    }

                    state.error != null && state.videos.isEmpty() -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ErrorScreen(
                                message = state.error!!,
                                onRetry = onRetry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp)
                            )
                        }
                    }

                    state.videos.isEmpty() && !state.isLoading -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                isSearchMode = state.isSearchMode,
                                keyword = state.searchKeyword,
                                onClearSearch = onClearSearch,
                                onUseRecommendedKeyword = onQuickSearch
                            )
                        }
                    }

                    else -> {
                        items(
                            items = state.videos,
                            key = { it.id }
                        ) { video ->
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.id) }
                            )
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "加载中...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (!state.isLoadingMore && state.videos.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "—— 到底了 ——",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!state.isSearchMode) {
                PullRefreshIndicator(
                    refreshing = state.isRefreshing,
                    state = pullRefreshState,
                    contentColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    LaunchedEffect(
        gridState,
        state.videos.size,
        state.isLoading,
        state.isLoadingMore,
        state.hasMorePages,
        state.isSearchMode
    ) {
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible to totalItems
        }.collect { (lastVisible, totalItems) ->
            if (lastVisible != null &&
                state.videos.isNotEmpty() &&
                totalItems > 0 &&
                lastVisible >= totalItems - 4 &&
                !state.isLoading &&
                !state.isLoadingMore &&
                state.hasMorePages
            ) {
                onLoadMore()
            }
        }
    }
}
