package com.icinema.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import com.icinema.pages.home.HomeContract.SortMode

@Composable
internal fun MinePage(
    state: HomeContract.UiState,
    onOpenHistory: () -> Unit,
    onOpenFavorite: () -> Unit,
    onVideoClick: (Long) -> Unit,
    onContinueWatchingClick: (Long, String, Int) -> Unit,
    onSortChange: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader(sectionTitle = "我的")

        MineHeroCard(
            historyCount = state.historyCount,
            continueWatchingCount = state.continueWatching.size,
            recommendationCount = state.recommendedVideos.size
        )

        QuickEntryRow(
            historyCount = state.historyCount,
            onOpenHistory = onOpenHistory,
            onOpenFavorite = onOpenFavorite
        )

        ContinueWatchingSection(
            items = state.continueWatching,
            onContinueWatchingClick = onContinueWatchingClick
        )

        if (state.recommendedVideos.isNotEmpty()) {
            RecommendationSection(
                videos = state.recommendedVideos,
                sortMode = state.sortMode,
                onVideoClick = onVideoClick,
                onSortChange = onSortChange
            )
        } else {
            EmptySectionCard(
                title = "暂无猜你喜欢内容",
                subtitle = "先去看看历史或收藏，系统会逐步为你整理推荐"
            )
        }
    }
}

@Composable
private fun MineHeroCard(
    historyCount: Int,
    continueWatchingCount: Int,
    recommendationCount: Int
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 5.dp,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "继续你的观影旅程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "把最近看过、想看的内容和系统推荐都收纳在这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetricCard(label = "观看历史", value = historyCount.toString(), modifier = Modifier.weight(1f))
                HeroMetricCard(label = "继续观看", value = continueWatchingCount.toString(), modifier = Modifier.weight(1f))
                HeroMetricCard(label = "今日推荐", value = recommendationCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickEntryRow(
    historyCount: Int,
    onOpenHistory: () -> Unit,
    onOpenFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EntryCard(
            title = "我的收藏",
            subtitle = "回看已收藏的视频",
            meta = "随时回到喜欢的片单",
            icon = Icons.Outlined.FavoriteBorder,
            onClick = onOpenFavorite,
            modifier = Modifier.weight(1f)
        )
        EntryCard(
            title = "观看历史",
            subtitle = "已记录 $historyCount 条观影轨迹",
            meta = "从上次停下的地方继续",
            icon = Icons.Outlined.History,
            onClick = onOpenHistory,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EntryCard(
    title: String,
    subtitle: String,
    meta: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    items: List<WatchHistoryItem>,
    onContinueWatchingClick: (Long, String, Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "继续观看",
                subtitle = if (items.isEmpty()) "还没有未看完的视频" else "从上次停下的位置继续"
            )

            if (items.isEmpty()) {
                EmptySectionCard(
                    title = "暂无继续观看内容",
                    subtitle = "开始播放一部影片后，这里会保留你的进度。"
                )
            } else {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = {
                                onContinueWatchingClick(item.videoId, item.sourceKey, item.episodeIndex)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: WatchHistoryItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(238.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(modifier = Modifier.height(140.dp)) {
                AsyncImage(
                    model = item.videoPic,
                    contentDescription = item.videoName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
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
                        .size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "继续观看",
                        modifier = Modifier.padding(8.dp),
                        tint = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.videoName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.episodeTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (item.completed) {
                        "已看完"
                    } else {
                        "观看至 ${(item.progress * 100).toInt()}%"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = if (item.completed) "查看详情" else "继续观看",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "猜你喜欢",
                subtitle = "基于你的浏览与观看行为整理"
            )
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(videos, key = { it.id }) { video ->
                RecommendationCard(
                    video = video,
                    onClick = { onVideoClick(video.id) }
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    video: Video,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(220.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            AsyncImage(
                model = video.pic,
                contentDescription = video.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = video.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
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

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
