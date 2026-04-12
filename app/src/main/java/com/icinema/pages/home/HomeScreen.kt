package com.icinema.pages.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import android.content.Intent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

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

/**
 * 吸顶标题栏 - 始终显示在顶部（沉浸式样式）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    isSearchMode: Boolean,
    searchKeyword: String,
    onSearchClick: () -> Unit,
    onClearSearch: () -> Unit,
    onOpenFavorite: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCategoryEditor: () -> Unit,
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    // 搜索模式下的返回按钮
                    AnimatedVisibility(
                        visible = isSearchMode,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        IconButton(onClick = {
                            onClearSearch()
                            onSearchToggle()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "返回列表",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // 标题
                    AnimatedContent(
                        targetState = isSearchMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "home_header_title"
                    ) { searchMode ->
                        Text(
                            text = if (searchMode) "搜索: $searchKeyword" else "iCinema",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 功能按钮（图标样式）
                    if (!isSearchMode) {
                        IconButton(onClick = onOpenFavorite) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onOpenHistory) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "历史",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onOpenCategoryEditor) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "编辑分类",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 搜索按钮
                    IconButton(onClick = onSearchToggle) {
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
        }
    }
}

/**
 * 搜索区域 - 可展开/收起
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBar(
    isExpanded: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearQuery: () -> Unit,
    searchHistory: List<String>,
    hotKeywords: List<String>,
    onQuickSearch: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    isSearching: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 搜索输入框
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索视频名称、演员、导演...") },
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
                    shape = RoundedCornerShape(24.dp),  // 更大的圆角
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
                                enabled = query.isNotBlank() && !isSearching
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

                // 搜索历史
                if (searchHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
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
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("搜索历史", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(onClick = onClearSearchHistory) { Text("清空") }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { keyword ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onQuickSearch(keyword) },
                                    label = { Text(keyword) }
                                )
                            }
                        }
                    }
                }

                // 热词推荐
                if (hotKeywords.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Whatshot,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text("热门搜索", style = MaterialTheme.typography.labelMedium)
                        }
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
    }
}

/**
 * 分类标签栏 - 吸顶显示
 */
@Composable
private fun CategoryBar(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    if (categories.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    leadingIcon = if (selectedCategoryId == null) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    label = { Text("全部") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            items(categories) { category ->
                val categoryIcon = getCategoryIcon(category.name)
                val isSelected = selectedCategoryId == category.id

                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category.id) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * 根据分类名称获取对应图标
 */
private fun getCategoryIcon(categoryName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        categoryName.contains("电影") -> Icons.Outlined.Movie
        categoryName.contains("剧") || categoryName.contains("连续") -> Icons.Outlined.LiveTv
        categoryName.contains("动漫") || categoryName.contains("动画") || categoryName.contains("卡通") -> Icons.Outlined.Animation
        categoryName.contains("综艺") || categoryName.contains("娱乐") -> Icons.Outlined.Mic
        categoryName.contains("纪录") -> Icons.Outlined.VideoLibrary
        categoryName.contains("短") -> Icons.Outlined.FiberManualRecord
        else -> Icons.Outlined.Category
    }
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

/**
 * 带动画效果的视频卡片 - 首屏加载时显示入场动画
 */
@Composable
private fun AnimatedVideoCard(
    video: Video,
    index: Int,
    isFirstLoad: Boolean,
    onClick: () -> Unit,
    onFavorite: (Video) -> Unit,
    onShare: (Video) -> Unit
) {
    if (isFirstLoad) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(index * 50L)
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        ) {
            VideoCard(
                video = video,
                onClick = onClick,
                onFavorite = onFavorite,
                onShare = onShare
            )
        }
    } else {
        VideoCard(
            video = video,
            onClick = onClick,
            onFavorite = onFavorite,
            onShare = onShare
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavorite: (Video) -> Unit = {},
    onShare: (Video) -> Unit = {}
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按下时的缩放
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),  // 固定高度，确保统一
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            focusedElevation = 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
        ) {
            // 封面图片（填满整个卡片）
            if (video.pic.isNotEmpty()) {
                AsyncImage(
                    model = video.pic,
                    contentDescription = video.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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

            // 渐变遮罩（底部）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = 0.6f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // 中央播放按钮（半透明）
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }

            // 右下角：播放标签
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "播放",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            // 底部文字信息（覆盖在封面上）
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                video.typeName?.let { type ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = type,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 长按快捷菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("收藏") },
                onClick = {
                    showMenu = false
                    onFavorite(video)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("分享") },
                onClick = {
                    showMenu = false
                    onShare(video)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("详情") },
                onClick = {
                    showMenu = false
                    onClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}

@Composable
private fun VideoCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),  // 固定高度，与真实卡片一致
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}

/**
 * 闪烁骨架屏效果
 */
@Composable
private fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_animation"
    )

    return this.then(
        Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                startX = -1000f * (1f - shimmer),
                endX = 1000f * shimmer
            )
        )
    )
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 固定吸顶的标题栏（延伸到状态栏）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(WindowInsets.statusBars.asPaddingValues())
            ) {
                HomeTopBar(
                    isSearchMode = state.isSearchMode,
                    searchKeyword = state.searchKeyword,
                    onSearchClick = { isSearchActive = true },
                    onClearSearch = onClearSearch,
                    onOpenFavorite = onOpenFavorite,
                    onOpenHistory = onOpenHistory,
                    onOpenCategoryEditor = onOpenCategoryEditor,
                    isSearchActive = isSearchActive,
                    onSearchToggle = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }
                    }
                )
            }

            // 搜索区域（可收起）
            SearchBar(
                isExpanded = isSearchActive,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        onSearch(searchQuery)
                        focusManager.clearFocus()
                    }
                },
                onClearQuery = { searchQuery = "" },
                searchHistory = state.searchHistory,
                hotKeywords = state.hotKeywords,
                onQuickSearch = onQuickSearch,
                onClearSearchHistory = onClearSearchHistory,
                isSearching = state.isSearching,
                focusManager = focusManager
            )

            // 分类标签栏（非搜索模式下显示）
            if (!state.isSearchMode) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryBar(
                        categories = state.visibleCategories,
                        selectedCategoryId = state.selectedCategoryId,
                        onCategorySelected = onCategorySelected
                    )
                    // 切换分类时的 loading 提示
                    if (state.isLoading && state.currentPage == 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                                .align(Alignment.CenterEnd),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // 视频网格列表
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                    // 继续观看区域
                    if (!state.isSearchMode && state.continueWatching.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueWatchingSection(
                                items = state.continueWatching,
                                onContinueWatchingClick = onContinueWatchingClick,
                                onOpenHistory = onOpenHistory
                            )
                        }
                    }

                    // 推荐区域
                    if (!state.isSearchMode && state.recommendedVideos.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RecommendationSection(
                                videos = state.recommendedVideos,
                                sortMode = state.sortMode,
                                onVideoClick = onVideoClick,
                                onSortChange = onSortChange
                            )
                        }
                    }

                    when {
                        state.isLoading && state.videos.isEmpty() -> {
                            // 首屏加载：显示骨架屏（3列 x 2行 = 6个卡片）
                            items(6) { index ->
                                VideoCardSkeleton()
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
                            val isFirstLoad = state.currentPage == 1 && state.videos.size <= 9

                            items(
                                items = state.videos,
                                key = { it.id }
                            ) { video ->
                                val index = state.videos.indexOf(video)
                                AnimatedVideoCard(
                                    video = video,
                                    index = index,
                                    isFirstLoad = isFirstLoad && index < 9,
                                    onClick = { onVideoClick(video.id) },
                                    onFavorite = { 
                                        scope.launch {
                                            snackbarHostState.showSnackbar("已收藏: ${it.name}")
                                        }
                                    },
                                    onShare = { 
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "来看看《${it.name}》")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }
                                )
                            }

                            // 加载更多：在底部显示骨架屏 + 文字提示
                            if (state.isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "正在加载更多...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                items(3) { index ->
                                    VideoCardSkeleton()
                                }
                            }

                            if (!state.isLoadingMore && state.videos.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "—— 已经到底啦 ——",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = "共 ${state.videos.size} 个视频",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 下拉刷新指示器
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
