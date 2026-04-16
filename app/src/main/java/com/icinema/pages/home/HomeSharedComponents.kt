package com.icinema.pages.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icinema.domain.model.Video
import kotlinx.coroutines.launch

@Composable
internal fun PageHeader(
    sectionTitle: String,
    actions: @Composable () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "iCinema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun VideoGrid(
    videos: List<Video>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    hasMorePages: Boolean,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onRefresh: (() -> Unit)?,
    onVideoClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    emptyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enablePullRefresh: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val pullRefreshState = if (enablePullRefresh && onRefresh != null) {
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = onRefresh
        )
    } else {
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (pullRefreshState != null) {
                        Modifier.pullRefresh(pullRefreshState)
                    } else {
                        Modifier
                    }
                ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                isLoading && videos.isEmpty() -> {
                    items(6) {
                        VideoCardSkeleton()
                    }
                }

                error != null && videos.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SimpleErrorState(
                            message = error,
                            onRetry = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        )
                    }
                }

                videos.isEmpty() && !isLoading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            emptyContent()
                        }
                    }
                }

                else -> {
                    items(
                        items = videos,
                        key = { it.id }
                    ) { video ->
                        VideoCard(
                            video = video,
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

                    if (isLoadingMore) {
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
                        items(3) {
                            VideoCardSkeleton()
                        }
                    }

                    if (!isLoadingMore && videos.isNotEmpty() && !hasMorePages) {
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
                                    text = "共 ${videos.size} 个视频",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pullRefreshState != null) {
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                contentColor = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    LaunchedEffect(
        gridState,
        videos.size,
        isLoading,
        isLoadingMore,
        hasMorePages
    ) {
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible to totalItems
        }.collect { (lastVisible, totalItems) ->
            if (lastVisible != null &&
                videos.isNotEmpty() &&
                totalItems > 0 &&
                lastVisible >= totalItems - 4 &&
                !isLoading &&
                !isLoadingMore &&
                hasMorePages
            ) {
                onLoadMore()
            }
        }
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
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
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
                    val scale = if (isPressed) 0.96f else 1f
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
                            )
                        )
                    )
            )

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
internal fun VideoCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        )
    }
}

@Composable
internal fun SimpleErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
internal fun SimpleEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
