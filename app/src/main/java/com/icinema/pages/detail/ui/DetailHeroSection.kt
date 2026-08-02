package com.icinema.pages.detail.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icinema.R
import com.icinema.domain.model.Video
import com.icinema.pages.cleanHtmlContent

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailHeroSection(
    video: Video,
    playGroups: List<Pair<String, List<Pair<String, String>>>>,
    currentSource: String?,
    selectedEpisode: Pair<String, String>?,
    episodeCount: Int,
    onSelectPlaySource: (String) -> Unit,
    onOpenCurrentEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                PosterImage(
                    imageUrl = video.coverImageUrl(),
                    title = video.name
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(148.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaySourceSelector(
                        playGroups = playGroups,
                        currentSource = currentSource,
                        onSelectPlaySource = onSelectPlaySource
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOfNotNull(
                            video.typeName?.takeIf { it.isNotBlank() },
                            video.year?.takeIf { it.isNotBlank() },
                            video.area?.takeIf { it.isNotBlank() },
                            episodeCount.takeIf { it > 0 }?.let { "$it 集" }
                        ).forEach { label ->
                            CompactMetaChip(label = label)
                        }
                    }

                    CurrentEpisodeLine(
                        currentSource = currentSource,
                        selectedEpisode = selectedEpisode,
                        enabled = selectedEpisode != null && currentSource != null,
                        onClick = onOpenCurrentEpisode
                    )
                }
            }

            Text(
                text = video.content.cleanDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PosterImage(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(104.dp)
            .height(148.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.tktk_house_cabinet_seat),
                error = painterResource(R.drawable.tktk_house_cabinet_seat)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.tktk_house_cabinet_seat),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun PlaySourceSelector(
    playGroups: List<Pair<String, List<Pair<String, String>>>>,
    currentSource: String?,
    onSelectPlaySource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LiveTv,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "播放源",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(
                items = playGroups,
                key = { _, item -> item.first }
            ) { index, (source, _) ->
                FilterChip(
                    selected = source == currentSource,
                    onClick = { onSelectPlaySource(source) },
                    label = {
                        Text(
                            text = source.ifBlank { "来源 ${index + 1}" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

@Composable
private fun CompactMetaChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CurrentEpisodeLine(
    currentSource: String?,
    selectedEpisode: Pair<String, String>?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = buildString {
                    append(currentSource?.takeIf { it.isNotBlank() } ?: "暂无播放源")
                    append(" · ")
                    append(selectedEpisode?.first?.takeIf { it.isNotBlank() } ?: "未选择剧集")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun Video.coverImageUrl(): String {
    return picThumb?.takeIf { it.isNotBlank() }
        ?: pic.takeIf { it.isNotBlank() }
        ?: ""
}

private fun String?.cleanDescription(): String {
    return this?.cleanHtmlContent().orEmpty().ifBlank { "暂无简介信息。" }
}
