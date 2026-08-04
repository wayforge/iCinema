package com.icinema.pages.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icinema.pages.widgets.EpisodePicker
import com.icinema.pages.widgets.EpisodePickerItem

@Composable
internal fun DetailPlaybackSection(
    currentSource: String?,
    currentEpisodes: List<Pair<String, String>>,
    selectedRange: Int,
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
                val pickerItems = remember(currentEpisodes) {
                    currentEpisodes.mapIndexed { index, episode ->
                        EpisodePickerItem(
                            index = index,
                            title = episode.first,
                            canCast = episode.second.contains("m3u8", ignoreCase = true)
                        )
                    }
                }
                EpisodePicker(
                    episodes = pickerItems,
                    selectedEpisode = selectedEpisode,
                    selectedRange = selectedRange,
                    onSelectRange = onSelectRange,
                    onSelectEpisode = onSelectEpisode,
                    onCopyEpisodeLink = { index, title ->
                        val url = currentEpisodes.getOrNull(index)?.second.orEmpty()
                        if (url.isNotBlank()) {
                            onCopyEpisodeLink(title.ifBlank { "第${index + 1}集" }, url)
                        }
                    },
                    onCastEpisode = onCastEpisode,
                    showCastActions = true
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

private fun playbackSubtitle(
    currentSource: String?,
    episodeCount: Int
): String {
    if (episodeCount <= 0) return "当前视频没有可用播放源"
    val source = currentSource?.takeIf { it.isNotBlank() }
    return listOfNotNull(source, "$episodeCount 集可播放").joinToString(" · ")
}
