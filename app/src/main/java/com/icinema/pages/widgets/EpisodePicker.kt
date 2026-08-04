package com.icinema.pages.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class EpisodePickerItem(
    val index: Int,
    val title: String,
    val canCast: Boolean = false
)

@Composable
fun EpisodePicker(
    episodes: List<EpisodePickerItem>,
    selectedEpisode: Int,
    selectedRange: Int,
    onSelectRange: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCopyEpisodeLink: ((index: Int, title: String) -> Unit)? = null,
    onCastEpisode: ((Int) -> Unit)? = null,
    showCastActions: Boolean = onCastEpisode != null,
    denseColumns: Int = 6,
    infoColumns: Int = 2
) {
    if (episodes.isEmpty()) return

    val total = episodes.size
    val rangeSize = remember(total) { episodeRangeSize(total) }
    val totalRanges = remember(total, rangeSize) { episodeRangeCount(total, rangeSize) }
    val clampedRange = selectedRange.coerceIn(0, (totalRanges - 1).coerceAtLeast(0))
    val bounds = remember(clampedRange, total, rangeSize) {
        episodeRangeBounds(clampedRange, total, rangeSize)
    }
    val rangeEpisodes = remember(episodes, bounds) {
        if (bounds.isEmpty()) emptyList() else episodes.subList(bounds.first, bounds.last + 1)
    }
    val dense = remember(episodes) { isDenseEpisodeLayout(episodes.map { it.title }) }
    val selectedInRange = selectedEpisode in bounds

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (totalRanges > 1) {
            EpisodeRangeBar(
                totalEpisodes = total,
                rangeSize = rangeSize,
                totalRanges = totalRanges,
                selectedRange = clampedRange,
                onSelectRange = onSelectRange
            )
        }

        if (dense) {
            DenseEpisodeGrid(
                rangeEpisodes = rangeEpisodes,
                selectedEpisode = if (selectedInRange) selectedEpisode else -1,
                columns = denseColumns,
                showCastActions = showCastActions,
                onSelectEpisode = onSelectEpisode,
                onCopyEpisodeLink = onCopyEpisodeLink,
                onCastEpisode = onCastEpisode
            )
        } else {
            InfoEpisodeGrid(
                rangeEpisodes = rangeEpisodes,
                selectedEpisode = if (selectedInRange) selectedEpisode else -1,
                columns = infoColumns,
                showCastActions = showCastActions,
                onSelectEpisode = onSelectEpisode,
                onCopyEpisodeLink = onCopyEpisodeLink,
                onCastEpisode = onCastEpisode
            )
        }
    }
}

@Composable
private fun EpisodeRangeBar(
    totalEpisodes: Int,
    rangeSize: Int,
    totalRanges: Int,
    selectedRange: Int,
    onSelectRange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedRange, totalRanges) {
        if (totalRanges <= 0) return@LaunchedEffect
        val target = selectedRange.coerceIn(0, totalRanges - 1)
        val visible = listState.layoutInfo.visibleItemsInfo
        val alreadyVisible = visible.any { it.index == target }
        if (!alreadyVisible) {
            listState.animateScrollToItem(target)
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(totalRanges, key = { it }) { rangeIndex ->
            val start = rangeIndex * rangeSize + 1
            val end = minOf((rangeIndex + 1) * rangeSize, totalEpisodes)
            FilterChip(
                selected = rangeIndex == selectedRange,
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

@Composable
private fun DenseEpisodeGrid(
    rangeEpisodes: List<EpisodePickerItem>,
    selectedEpisode: Int,
    columns: Int,
    showCastActions: Boolean,
    onSelectEpisode: (Int) -> Unit,
    onCopyEpisodeLink: ((index: Int, title: String) -> Unit)?,
    onCastEpisode: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cols = columns.coerceAtLeast(1)
    var menuEpisodeIndex by remember { mutableStateOf<Int?>(null) }
    val hasMenuActions = onCopyEpisodeLink != null || (showCastActions && onCastEpisode != null)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rangeEpisodes.chunked(cols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { episode ->
                    val titleLabel = episode.title.ifBlank { "第${episode.index + 1}集" }
                    DenseEpisodeCell(
                        label = denseEpisodeLabel(episode.title, episode.index),
                        menuTitle = titleLabel,
                        isSelected = episode.index == selectedEpisode,
                        menuExpanded = menuEpisodeIndex == episode.index,
                        onMenuExpandChange = { expanded ->
                            menuEpisodeIndex = if (expanded) episode.index else null
                        },
                        canCopy = onCopyEpisodeLink != null,
                        canCast = showCastActions && episode.canCast && onCastEpisode != null,
                        enableLongPressMenu = hasMenuActions,
                        onClick = { onSelectEpisode(episode.index) },
                        onPlay = {
                            menuEpisodeIndex = null
                            onSelectEpisode(episode.index)
                        },
                        onCopy = {
                            menuEpisodeIndex = null
                            onCopyEpisodeLink?.invoke(episode.index, titleLabel)
                        },
                        onCast = {
                            menuEpisodeIndex = null
                            onCastEpisode?.invoke(episode.index)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(cols - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DenseEpisodeCell(
    label: String,
    menuTitle: String,
    isSelected: Boolean,
    menuExpanded: Boolean,
    onMenuExpandChange: (Boolean) -> Unit,
    canCopy: Boolean,
    canCast: Boolean,
    enableLongPressMenu: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onCopy: () -> Unit,
    onCast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .then(
                    if (enableLongPressMenu) {
                        Modifier.combinedClickable(
                            onClickLabel = "播放 $label",
                            onLongClickLabel = "更多操作",
                            onClick = onClick,
                            onLongClick = { onMenuExpandChange(true) }
                        )
                    } else {
                        Modifier.clickable(onClickLabel = "播放 $label", onClick = onClick)
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) {
                scheme.primary.copy(alpha = 0.16f)
            } else {
                scheme.surfaceVariant.copy(alpha = 0.55f)
            },
            contentColor = if (isSelected) {
                scheme.primary
            } else {
                scheme.onSurface
            }
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuExpandChange(false) },
            offset = DpOffset(0.dp, 4.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = scheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            DenseEpisodeActionPopup(
                title = menuTitle,
                canCopy = canCopy,
                canCast = canCast,
                onPlay = onPlay,
                onCopy = onCopy,
                onCast = onCast
            )
        }
    }
}

@Composable
private fun DenseEpisodeActionPopup(
    title: String,
    canCopy: Boolean,
    canCast: Boolean,
    onPlay: () -> Unit,
    onCopy: () -> Unit,
    onCast: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = scheme.primaryContainer.copy(alpha = 0.55f),
            contentColor = scheme.onPrimaryContainer
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DenseActionButton(
                icon = Icons.Filled.PlayArrow,
                label = "播放",
                containerColor = scheme.primary.copy(alpha = 0.18f),
                contentColor = scheme.primary,
                onClick = onPlay,
                modifier = Modifier.weight(1f)
            )
            if (canCopy) {
                DenseActionButton(
                    icon = Icons.Filled.ContentCopy,
                    label = "复制",
                    containerColor = scheme.secondaryContainer.copy(alpha = 0.72f),
                    contentColor = scheme.onSecondaryContainer,
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                )
            }
            if (canCast) {
                DenseActionButton(
                    icon = Icons.Filled.Cast,
                    label = "投屏",
                    containerColor = scheme.tertiaryContainer.copy(alpha = 0.72f),
                    contentColor = scheme.onTertiaryContainer,
                    onClick = onCast,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DenseActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClickLabel = label, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InfoEpisodeGrid(
    rangeEpisodes: List<EpisodePickerItem>,
    selectedEpisode: Int,
    columns: Int,
    showCastActions: Boolean,
    onSelectEpisode: (Int) -> Unit,
    onCopyEpisodeLink: ((index: Int, title: String) -> Unit)?,
    onCastEpisode: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cols = columns.coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rangeEpisodes.chunked(cols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { episode ->
                    val label = episode.title.ifBlank { "第${episode.index + 1}集" }
                    InfoEpisodeCell(
                        label = label,
                        isSelected = episode.index == selectedEpisode,
                        canCast = showCastActions && episode.canCast,
                        onPlayClick = { onSelectEpisode(episode.index) },
                        onCopyLink = onCopyEpisodeLink?.let { handler ->
                            { handler(episode.index, label) }
                        },
                        onCastClick = if (showCastActions) {
                            { onCastEpisode?.invoke(episode.index) }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(cols - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoEpisodeCell(
    label: String,
    isSelected: Boolean,
    canCast: Boolean,
    onPlayClick: () -> Unit,
    onCopyLink: (() -> Unit)?,
    onCastClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .then(
                if (onCopyLink != null) {
                    Modifier.combinedClickable(
                        onClickLabel = "播放 $label",
                        onLongClickLabel = "复制 $label 链接",
                        onClick = onPlayClick,
                        onLongClick = onCopyLink
                    )
                } else {
                    Modifier.clickable(onClickLabel = "播放 $label", onClick = onPlayClick)
                }
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
            if (onCastClick != null) {
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
