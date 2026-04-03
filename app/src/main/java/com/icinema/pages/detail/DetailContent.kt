package com.icinema.pages.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icinema.domain.model.Video
import com.icinema.pages.cleanHtmlContent
import com.icinema.pages.widgets.ErrorScreen
import com.icinema.pages.widgets.LoadingScreen
import com.icinema.ui.theme.iCinemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    state: DetailContract.UiState,
    onBackClick: () -> Unit,
    onIntent: (DetailContract.UiIntent) -> Unit,
    onOpenPlayer: (sourceKey: String?, episodeIndex: Int) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.video?.name ?: "视频详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            state.isLoading -> {
                LoadingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.error != null -> {
                ErrorScreen(
                    message = state.error,
                    onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.video != null -> {
                DetailSuccessContent(
                    state = state,
                    onIntent = onIntent,
                    onOpenPlayer = onOpenPlayer,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                ErrorScreen(
                    message = "详情暂不可用",
                    onRetry = { onIntent(DetailContract.UiIntent.RetryLoad) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailSuccessContent(
    state: DetailContract.UiState,
    onIntent: (DetailContract.UiIntent) -> Unit,
    onOpenPlayer: (sourceKey: String?, episodeIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val video = state.video ?: return
    val playGroups = video.playGroups.filter { it.second.isNotEmpty() }
    val currentSource = state.selectedPlaySource ?: playGroups.firstOrNull()?.first
    val currentEpisodes = playGroups.firstOrNull { it.first == currentSource }?.second.orEmpty()
    val rangeSize = when {
        currentEpisodes.size > 100 -> 30
        currentEpisodes.size > 40 -> 20
        else -> 12
    }
    val totalRanges = if (currentEpisodes.isEmpty()) 0 else (currentEpisodes.size + rangeSize - 1) / rangeSize
    val clampedRange = state.selectedRange.coerceIn(0, (totalRanges - 1).coerceAtLeast(0))
    val startIndex = clampedRange * rangeSize
    val endIndex = minOf(startIndex + rangeSize, currentEpisodes.size)
    val rangeEpisodes = if (currentEpisodes.isEmpty()) emptyList() else currentEpisodes.subList(startIndex, endIndex)
    val selectedEpisode = currentEpisodes.getOrNull(state.selectedEpisode)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AsyncImage(
                        model = video.picThumb ?: video.pic,
                        contentDescription = video.name,
                        modifier = Modifier
                            .width(118.dp)
                            .height(170.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = video.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOfNotNull(video.typeName, video.year, video.area).forEach { label ->
                                MetaChip(label = label)
                            }
                            if (currentEpisodes.isNotEmpty()) {
                                MetaChip(label = "${currentEpisodes.size} 集")
                            }
                        }

                        video.director?.takeIf { it.isNotBlank() }?.let { value ->
                            InfoLine(label = "导演", value = value)
                        }
                        video.actor?.takeIf { it.isNotBlank() }?.let { value ->
                            InfoLine(label = "演员", value = value)
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "当前选集",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = buildString {
                                        append(currentSource ?: "暂无播放源")
                                        append(" · ")
                                        append(selectedEpisode?.first ?: "未选择剧集")
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = selectedEpisode?.second ?: "当前视频未提供可播放地址",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                                if (selectedEpisode != null && currentSource != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onOpenPlayer(currentSource, state.selectedEpisode)
                                        }
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("立即播放")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionTitle("播放编排")

                if (playGroups.isEmpty()) {
                    Text(
                        text = "当前视频没有可用播放源。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playGroups) { (source, _) ->
                            FilterChip(
                                selected = source == currentSource,
                                onClick = { onIntent(DetailContract.UiIntent.SelectPlaySource(source)) },
                                label = { Text(source) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.LiveTv,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    if (totalRanges > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(totalRanges) { rangeIndex ->
                                val start = rangeIndex * rangeSize + 1
                                val end = minOf((rangeIndex + 1) * rangeSize, currentEpisodes.size)
                                FilterChip(
                                    selected = rangeIndex == clampedRange,
                                    onClick = { onIntent(DetailContract.UiIntent.SelectRange(rangeIndex)) },
                                    label = { Text("$start-$end") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rangeEpisodes.forEachIndexed { localIndex, episode ->
                            val actualIndex = startIndex + localIndex
                            EpisodeTag(
                                label = episode.first.ifBlank { "第${actualIndex + 1}集" },
                                isSelected = actualIndex == state.selectedEpisode,
                                onClick = {
                                    onIntent(DetailContract.UiIntent.SelectEpisode(actualIndex))
                                    onOpenPlayer(currentSource, actualIndex)
                                }
                            )
                        }
                    }
                }
            }
        }

        ElevatedCard(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("剧情简介")
                    Button(onClick = { onIntent(DetailContract.UiIntent.RetryLoad) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("刷新")
                    }
                }
                Text(
                    text = video.content?.cleanHtmlContent().orEmpty().ifBlank { "暂无简介信息。" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EpisodeTag(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun detailPreviewState(): DetailContract.UiState {
    val sampleVideo = Video(
        id = 1L,
        name = "无尽夜航",
        pic = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800",
        picThumb = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400",
        actor = "顾遥, 周沉, 林见川",
        director = "许舟",
        content = "<p>这是一段用于详情页预览的样板简介，展示海报、元信息、播放源切换与选集浏览的完整布局。</p>",
        area = "中国",
        year = "2026",
        typeId = 1,
        typeName = "悬疑",
        playFrom = "main\$\$\$backup",
        playUrl = listOf(
            List(18) { index ->
                "第${index + 1}集\$https://example.com/main/${index + 1}.m3u8"
            }.joinToString("#"),
            List(6) { index ->
                "第${index + 1}集\$https://example.com/backup/${index + 1}.m3u8"
            }.joinToString("#")
        ).joinToString("\$\$\$"),
        total = 18
    )

    return DetailContract.UiState(
        currentVideoId = 1L,
        isLoading = false,
        video = sampleVideo,
        selectedPlaySource = "main",
        selectedEpisode = 2,
        selectedRange = 0
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun DetailContentPreview() {
    iCinemaTheme {
        DetailContent(
            state = detailPreviewState(),
            onBackClick = {},
            onIntent = {},
            onOpenPlayer = { _, _ -> }
        )
    }
}
