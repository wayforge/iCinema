package com.icinema.pages.detail.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailPlaybackSection(
    currentSource: String?,
    currentEpisodes: List<Pair<String, String>>,
    totalRanges: Int,
    rangeSize: Int,
    clampedRange: Int,
    rangeEpisodes: List<Pair<String, String>>,
    startIndex: Int,
    selectedEpisode: Int,
    onSelectRange: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onCopyEpisodeLink: (label: String, url: String) -> Unit,
    onCastEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(
                text = "选集",
                subtitle = playbackSubtitle(
                    currentSource = currentSource,
                    episodeCount = currentEpisodes.size
                )
            )

            if (currentEpisodes.isEmpty()) {
                EmptyPlaybackMessage()
            } else {
                if (totalRanges > 1) {
                    EpisodeRangeSelector(
                        totalRanges = totalRanges,
                        rangeSize = rangeSize,
                        currentEpisodesCount = currentEpisodes.size,
                        clampedRange = clampedRange,
                        onSelectRange = onSelectRange
                    )
                }

                EpisodeGrid(
                    rangeEpisodes = rangeEpisodes,
                    startIndex = startIndex,
                    selectedEpisode = selectedEpisode,
                    onSelectEpisode = onSelectEpisode,
                    onCopyEpisodeLink = onCopyEpisodeLink,
                    onCastEpisode = onCastEpisode
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaybackMessage() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "当前视频没有可用播放源。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeRangeSelector(
    totalRanges: Int,
    rangeSize: Int,
    currentEpisodesCount: Int,
    clampedRange: Int,
    onSelectRange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlaybackSubHeader(
            icon = Icons.Filled.GridView,
            title = "分段"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalRanges) { rangeIndex ->
                val start = rangeIndex * rangeSize + 1
                val end = minOf((rangeIndex + 1) * rangeSize, currentEpisodesCount)
                FilterChip(
                    selected = rangeIndex == clampedRange,
                    onClick = { onSelectRange(rangeIndex) },
                    label = { Text("$start-$end") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeGrid(
    rangeEpisodes: List<Pair<String, String>>,
    startIndex: Int,
    selectedEpisode: Int,
    onSelectEpisode: (Int) -> Unit,
    onCopyEpisodeLink: (label: String, url: String) -> Unit,
    onCastEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlaybackSubHeader(
            icon = Icons.Filled.GridView,
            title = "剧集"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rangeEpisodes.chunked(2).forEachIndexed { rowIndex, rowEpisodes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowEpisodes.forEachIndexed { columnIndex, episode ->
                        val actualIndex = startIndex + rowIndex * 2 + columnIndex
                        val label = episode.first.ifBlank { "第${actualIndex + 1}集" }
                        EpisodeActionTag(
                            label = label,
                            isSelected = actualIndex == selectedEpisode,
                            canCast = episode.second.contains("m3u8", ignoreCase = true),
                            onPlayClick = { onSelectEpisode(actualIndex) },
                            onCopyLink = { onCopyEpisodeLink(label, episode.second) },
                            onCastClick = { onCastEpisode(actualIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(2 - rowEpisodes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeActionTag(
    label: String,
    isSelected: Boolean,
    canCast: Boolean,
    onPlayClick: () -> Unit,
    onCopyLink: () -> Unit,
    onCastClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .combinedClickable(
                onClickLabel = "播放 $label",
                onLongClickLabel = "复制 $label 链接",
                onClick = onPlayClick,
                onLongClick = onCopyLink
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            EpisodeIconAction(
                contentDescription = "播放 $label",
                onClick = onPlayClick,
                enabled = true,
                isSelected = isSelected,
                icon = Icons.Filled.PlayArrow
            )
            EpisodeIconAction(
                contentDescription = "投屏 $label",
                onClick = onCastClick,
                enabled = canCast,
                isSelected = isSelected,
                icon = Icons.Filled.Cast
            )
        }
    }
}

@Composable
private fun EpisodeIconAction(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier
            .size(32.dp)
            .clickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick
            )
            .padding(7.dp),
        tint = tint
    )
}

@Composable
private fun PlaybackSubHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun playbackSubtitle(
    currentSource: String?,
    episodeCount: Int
): String {
    if (episodeCount <= 0) return "当前视频没有可用播放源"
    val source = currentSource?.takeIf { it.isNotBlank() }
    return listOfNotNull(source, "$episodeCount 集可播放").joinToString(" · ")
}
