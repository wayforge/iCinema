package com.icinema.pages.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.icinema.pages.widgets.EmptyState
import com.icinema.pages.widgets.ErrorScreen
import com.icinema.pages.widgets.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeContract.UiEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is HomeContract.UiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = { viewModel.handleIntent(HomeContract.UiIntent.Refresh) },
        onVideoClick = onVideoClick,
        onRefresh = { viewModel.handleIntent(HomeContract.UiIntent.Refresh) },
        onCategorySelected = { categoryId ->
            viewModel.handleIntent(HomeContract.UiIntent.SelectCategory(categoryId))
        },
        onSearch = { keyword ->
            viewModel.handleIntent(HomeContract.UiIntent.Search(keyword))
        },
        onClearSearch = {
            viewModel.handleIntent(HomeContract.UiIntent.ClearSearch)
        },
        onLoadMore = { viewModel.handleIntent(HomeContract.UiIntent.LoadMore) }
    )
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
private fun VideoGridContent(
    videos: List<Video>,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    isLoadingMore: Boolean,
    onVideoClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        items(
            items = videos,
            key = { it.id }
        ) { video ->
            VideoCard(
                video = video,
                onClick = { onVideoClick(video.id) }
            )
        }

        if (isLoadingMore) {
            item {
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

        if (!isLoadingMore && videos.isNotEmpty()) {
            item {
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

// 将 HomeScreen 的 UI 内容提取为独立的可预览函数
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeContent(
    state: HomeContract.UiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onRefresh: () -> Unit,
    onCategorySelected: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLoadMore: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(state.searchKeyword) }
    var isSearchActive by remember { mutableStateOf(state.isSearchMode) }
    val focusManager = LocalFocusManager.current

    // 监听搜索关键词变化，同步UI状态
    LaunchedEffect(state.searchKeyword) {
        if (state.searchKeyword != searchQuery) {
            searchQuery = state.searchKeyword
        }
    }

    // 监听搜索模式变化，同步UI状态
    LaunchedEffect(state.isSearchMode) {
        isSearchActive = state.isSearchMode
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = state.isSearchMode,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "title_animation"
                        ) { isSearchMode ->
                            Text(
                                text = if (isSearchMode) "搜索: ${state.searchKeyword}" else "iCinema",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
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
                                    Icons.Default.Clear,
                                    contentDescription = "Back to list",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                            } else {
                                searchQuery = ""
                                isSearchActive = true
                            }
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                )

                AnimatedVisibility(
                    visible = !isSearchActive && state.categories.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CategoryTabs(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = onCategorySelected
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                        contentDescription = "Clear",
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
            }

            when {
                state.isLoading && state.videos.isEmpty() -> {
                    LoadingScreen()
                }
                state.error != null && state.videos.isEmpty() -> {
                    ErrorScreen(
                        message = state.error!!,
                        onRetry = onRetry
                    )
                }
                state.videos.isEmpty() && !state.isLoading -> {
                    EmptyState(
                        isSearchMode = state.isSearchMode,
                        keyword = state.searchKeyword,
                        onClearSearch = onClearSearch
                    )
                }
                else -> {
                    val gridState = rememberLazyStaggeredGridState()
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = state.isRefreshing,
                        onRefresh = onRefresh
                    )
                    
                    if (state.isSearchMode) {
                        VideoGridContent(
                            videos = state.videos,
                            gridState = gridState,
                            isLoadingMore = state.isLoadingMore,
                            onVideoClick = onVideoClick,
                            onLoadMore = onLoadMore
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            VideoGridContent(
                                videos = state.videos,
                                gridState = gridState,
                                isLoadingMore = state.isLoadingMore,
                                onVideoClick = onVideoClick,
                                onLoadMore = onLoadMore,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pullRefresh(pullRefreshState)
                            )

                            PullRefreshIndicator(
                                refreshing = state.isRefreshing,
                                state = pullRefreshState,
                                contentColor = MaterialTheme.colorScheme.primary,
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }

                    LaunchedEffect(
                        gridState,
                        state.videos.size,
                        state.isLoading,
                        state.isLoadingMore,
                        state.hasMorePages
                    ) {
                        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastIndex ->
                                if (lastIndex != null &&
                                    lastIndex >= state.videos.size - 4 &&
                                    !state.isLoading &&
                                    !state.isLoadingMore &&
                                    state.hasMorePages) {
                                    onLoadMore()
                                }
                            }
                    }
                }
            }
        }
    }
}
