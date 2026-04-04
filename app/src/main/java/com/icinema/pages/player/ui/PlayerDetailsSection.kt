package com.icinema.pages.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icinema.domain.model.PlaySource
import com.icinema.ui.theme.iCinemaTheme

@Composable
internal fun PlayerDetailsSection(
    state: PlayerContract.UiState,
    selectedSource: PlaySource?,
    onOpenSources: () -> Unit,
    onOpenEpisodes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PlayerHeadlineCard(
            title = state.video?.name.orEmpty(),
            selectedSourceKey = state.selectedSourceKey,
            currentEpisodeTitle = state.currentEpisode?.title,
            isPlaying = state.isPlaying,
            onOpenSources = onOpenSources,
            onOpenEpisodes = onOpenEpisodes
        )

        state.video?.content
            ?.replace(Regex("<[^>]*>"), "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { description ->
                PlayerDescriptionCard(description = description)
            }

        PlayerEpisodeSummaryCard(
            selectedSource = selectedSource,
            selectedEpisodeIndex = state.selectedEpisodeIndex,
            canPlayNext = state.canPlayNext
        )
    }
}

@Composable
private fun PlayerHeadlineCard(
    title: String,
    selectedSourceKey: String?,
    currentEpisodeTitle: String?,
    isPlaying: Boolean,
    onOpenSources: () -> Unit,
    onOpenEpisodes: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPlaying) "播放中" else "已暂停",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentEpisodeTitle ?: "未选择剧集",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = onOpenSources,
                    label = { Text(selectedSourceKey ?: "线路") },
                    leadingIcon = {
                        Icon(Icons.Filled.LiveTv, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = false,
                    onClick = onOpenEpisodes,
                    label = { Text("选集") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerDescriptionCard(description: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "剧情简介",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerEpisodeSummaryCard(
    selectedSource: PlaySource?,
    selectedEpisodeIndex: Int,
    canPlayNext: Boolean
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "当前播放",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = buildString {
                    append(selectedSource?.key ?: "暂无线路")
                    append(" · ")
                    append(selectedSource?.episodes?.getOrNull(selectedEpisodeIndex)?.title ?: "未选择剧集")
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (canPlayNext) "可继续下一集" else "已是最后一集",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412)
@Composable
private fun PlayerDetailsSectionPreview() {
    iCinemaTheme {
        PlayerDetailsSection(
            state = PlayerPreviewData.state(),
            selectedSource = PlayerPreviewData.playSources.first(),
            onOpenSources = {},
            onOpenEpisodes = {}
        )
    }
}
