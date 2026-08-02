package com.icinema.cast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icinema.cast.CastDevice
import com.icinema.cast.CastOverlayViewModel
import com.icinema.cast.CastState
import com.icinema.ui.theme.iCinemaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastMiniControllerHost(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp,
    viewModel: CastOverlayViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visible = state.isCasting || state.isConnecting
    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isCasting) {
        while (isActive && state.isCasting) {
            viewModel.refreshPlaybackPosition()
            delay(5_000L)
        }
    }

    LaunchedEffect(visible) {
        if (!visible) {
            expanded = false
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CastMiniController(
                state = state,
                onTogglePlayPause = viewModel::togglePlayPause,
                onStopCasting = viewModel::stopCasting,
                onOpenDetails = { expanded = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = bottomPadding)
            )
        }
    }

    if (expanded && visible) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false }
        ) {
            CastExpandedControllerPanel(
                state = state,
                onTogglePlayPause = viewModel::togglePlayPause,
                onRefreshPlaybackPosition = viewModel::refreshPlaybackPosition,
                onStopCasting = viewModel::stopCasting,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
fun CastMiniController(
    state: CastState,
    onTogglePlayPause: () -> Unit,
    onStopCasting: () -> Unit,
    onOpenDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val deviceName = state.connectedDevice?.name.orEmpty()
    val title = castTitle(state)
    val subtitle = castSubtitle(state, includeHint = onOpenDetails != null)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onOpenDetails != null) {
                    Modifier.clickable(
                        onClickLabel = "展开投屏控制",
                        onClick = onOpenDetails
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            if (state.durationMs > 0L) {
                LinearProgressIndicator(
                    progress = {
                        state.currentPositionMs
                            .coerceIn(0L, state.durationMs)
                            .toFloat() / state.durationMs.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CastConnected,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                IconButton(
                    enabled = state.isCasting && !state.isConnecting,
                    onClick = onTogglePlayPause
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "暂停投屏" else "继续投屏"
                    )
                }
                if (onOpenDetails != null) {
                    IconButton(onClick = onOpenDetails) {
                        Icon(
                            imageVector = Icons.Filled.ExpandLess,
                            contentDescription = "展开投屏控制"
                        )
                    }
                }
                IconButton(onClick = onStopCasting) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "断开投屏"
                    )
                }
            }
        }
    }
}

@Composable
fun CastExpandedControllerPanel(
    state: CastState,
    onTogglePlayPause: () -> Unit,
    onRefreshPlaybackPosition: () -> Unit,
    onStopCasting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Filled.CastConnected,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = castTitle(state),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = castSubtitle(state, includeHint = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        CastProgressBlock(state = state)

        state.errorMessage?.let { message ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                enabled = state.isCasting && !state.isConnecting,
                onClick = onTogglePlayPause,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(if (state.isPlaying) "暂停" else "继续")
            }
            TextButton(
                enabled = state.isCasting && !state.isConnecting,
                onClick = onRefreshPlaybackPosition
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("刷新进度")
            }
            TextButton(onClick = onStopCasting) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("断开")
            }
        }
    }
}

@Composable
private fun CastProgressBlock(
    state: CastState
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.durationMs > 0L) {
            LinearProgressIndicator(
                progress = {
                    state.currentPositionMs
                        .coerceIn(0L, state.durationMs)
                        .toFloat() / state.durationMs.toFloat()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatCastTime(state.currentPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCastTime(state.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = if (state.isConnecting) "正在连接设备..." else "正在同步投屏进度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CastDeviceSheetContent(
    state: CastState,
    onRefresh: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onStopCasting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "投屏设备",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                enabled = !state.isSearching,
                onClick = onRefresh
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(if (state.isSearching) "搜索中" else "刷新")
            }
        }

        if (state.isSearching) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("正在搜索同一 Wi-Fi 下的电视")
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!state.isSearching && state.devices.isEmpty()) {
            Text(
                text = "未发现设备，请确认手机和小米电视在同一 Wi-Fi，并开启电视投屏/米联。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.connectedDevice?.let { device ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CastConnected,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "正在投屏到 ${device.name}",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onStopCasting) {
                        Text("断开")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.devices, key = { it.id }) { device ->
                val selected = device.id == state.connectedDevice?.id
                CastDeviceRow(
                    device = device,
                    selected = selected,
                    enabled = !state.isConnecting,
                    onClick = { onSelectDevice(device.id) }
                )
            }
        }
    }
}

@Composable
private fun CastDeviceRow(
    device: CastDevice,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
        }
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = device.name,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val description = listOf(device.manufacturer, device.modelName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun castTitle(state: CastState): String {
    return when {
        state.isConnecting -> "正在连接投屏设备"
        state.currentMediaTitle.isNotBlank() -> state.currentMediaTitle
        state.errorMessage != null -> "投屏异常"
        else -> "正在投屏"
    }
}

private fun castSubtitle(state: CastState, includeHint: Boolean): String {
    val deviceName = state.connectedDevice?.name.orEmpty()
    val status = when {
        state.isConnecting -> "连接中"
        state.isPlaying -> "播放中"
        state.isCasting -> "已暂停"
        else -> "准备投屏"
    }
    return listOf(
        deviceName,
        state.currentMediaSubtitle,
        status,
        if (includeHint) "点按展开" else ""
    )
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { if (includeHint) "点按展开投屏控制" else "已连接投屏设备" }
}

private fun formatCastTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412)
@Composable
private fun CastMiniControllerPreview() {
    iCinemaTheme {
        CastMiniController(
            state = CastState(
                connectedDevice = CastDevice("1", "投屏助手_MECO", "Xiaomi", "hyperDLNA"),
                isCasting = true,
                isPlaying = true,
                currentMediaTitle = "无尽夜航 - 第 3 集",
                currentMediaSubtitle = "main",
                currentPositionMs = 320_000,
                durationMs = 1_800_000
            ),
            onTogglePlayPause = {},
            onStopCasting = {},
            onOpenDetails = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412)
@Composable
private fun CastExpandedControllerPanelPreview() {
    iCinemaTheme {
        CastExpandedControllerPanel(
            state = CastState(
                connectedDevice = CastDevice("1", "投屏助手_MECO", "Xiaomi", "hyperDLNA"),
                isCasting = true,
                isPlaying = true,
                currentMediaTitle = "无尽夜航 - 第 3 集",
                currentMediaSubtitle = "main",
                currentPositionMs = 320_000,
                durationMs = 1_800_000
            ),
            onTogglePlayPause = {},
            onRefreshPlaybackPosition = {},
            onStopCasting = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412, heightDp = 520)
@Composable
private fun CastDeviceSheetContentPreview() {
    iCinemaTheme {
        CastDeviceSheetContent(
            state = CastState(
                devices = listOf(
                    CastDevice("1", "投屏助手_MECO", "Xiaomi", "hyperDLNA"),
                    CastDevice("2", "客厅电视", "Xiaomi", "MiTV")
                ),
                connectedDevice = CastDevice("1", "投屏助手_MECO", "Xiaomi", "hyperDLNA"),
                isCasting = true
            ),
            onRefresh = {},
            onSelectDevice = {},
            onStopCasting = {}
        )
    }
}
