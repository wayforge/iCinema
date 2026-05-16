package com.icinema.pages.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailPlaybackSection(
    playGroups: List<Pair<String, List<Pair<String, String>>>>,
    currentSource: String?,
    currentEpisodes: List<Pair<String, String>>,
    totalRanges: Int,
    rangeSize: Int,
    clampedRange: Int,
    rangeEpisodes: List<Pair<String, String>>,
    startIndex: Int,
    selectedEpisode: Int,
    onSelectPlaySource: (String) -> Unit,
    onSelectRange: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionTitle(
                text = "播放",
                subtitle = if (currentEpisodes.isEmpty()) "当前视频没有可用播放源" else "${currentEpisodes.size} 集可播放"
            )

            if (playGroups.isEmpty()) {
                EmptyPlaybackMessage()
            } else {
                PlaySourceRow(
                    playGroups = playGroups,
                    currentSource = currentSource,
                    onSelectPlaySource = onSelectPlaySource
                )

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
                    onSelectEpisode = onSelectEpisode
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaybackMessage() {
    Surface(
        shape = RoundedCornerShape(16.dp),
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

@Composable
private fun PlaySourceRow(
    playGroups: List<Pair<String, List<Pair<String, String>>>>,
    currentSource: String?,
    onSelectPlaySource: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PlaybackSubHeader(
            icon = Icons.Filled.LiveTv,
            title = "播放源"
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(
                items = playGroups,
                key = { _, item -> item.first }
            ) { index, (source, episodes) ->
                FilterChip(
                    selected = source == currentSource,
                    onClick = { onSelectPlaySource(source) },
                    label = { Text(source.ifBlank { "来源 ${index + 1}" }) },
                    trailingIcon = {
                        Text(
                            text = episodes.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
private fun EpisodeRangeSelector(
    totalRanges: Int,
    rangeSize: Int,
    currentEpisodesCount: Int,
    clampedRange: Int,
    onSelectRange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PlaybackSubHeader(
            icon = Icons.Filled.GridView,
            title = "分段"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalRanges) { rangeIndex ->
                val start = rangeIndex * rangeSize + 1
                val end = minOf((rangeIndex + 1) * rangeSize, currentEpisodesCount)
                FilterChip(
                    selected = rangeIndex == clampedRange,
                    onClick = { onSelectRange(rangeIndex) },
                    label = { Text("$start-$end") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
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
    onSelectEpisode: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PlaybackSubHeader(
            icon = Icons.Filled.GridView,
            title = "选集"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rangeEpisodes.forEachIndexed { localIndex, episode ->
                val actualIndex = startIndex + localIndex
                EpisodeTag(
                    label = episode.first.ifBlank { "第${actualIndex + 1}集" },
                    isSelected = actualIndex == selectedEpisode,
                    onClick = { onSelectEpisode(actualIndex) }
                )
            }
        }
    }
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
