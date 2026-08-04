package com.icinema.pages.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icinema.cast.ui.CastDeviceSheetContent
import com.icinema.domain.model.PlaySource
import com.icinema.domain.model.PlayableEpisode
import com.icinema.pages.widgets.EpisodePicker
import com.icinema.pages.widgets.EpisodePickerItem
import com.icinema.pages.widgets.episodeRangeIndex
import com.icinema.ui.theme.iCinemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSheetHost(
    sheetMode: PlayerContract.SheetMode?,
    state: PlayerContract.UiState,
    selectedSource: PlaySource?,
    onDismiss: () -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onOpenSources: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onRefreshCastDevices: () -> Unit,
    onSelectCastDevice: (String) -> Unit,
    onStopCasting: () -> Unit
) {
    if (sheetMode == null) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        when (sheetMode) {
            PlayerContract.SheetMode.Sources -> {
                SelectionSheet(
                    title = "切换线路",
                    items = state.playSources.map { it.key },
                    selectedItem = state.selectedSourceKey,
                    onSelect = onSelectSource
                )
            }

            PlayerContract.SheetMode.Episodes -> {
                EpisodeSelectionSheet(
                    episodes = selectedSource?.episodes.orEmpty(),
                    selectedEpisodeIndex = state.selectedEpisodeIndex,
                    onSelectEpisode = onSelectEpisode
                )
            }

            PlayerContract.SheetMode.CastDevices -> {
                CastDeviceSheetContent(
                    state = state.castState,
                    onRefresh = onRefreshCastDevices,
                    onSelectDevice = onSelectCastDevice,
                    onStopCasting = onStopCasting
                )
            }
        }
    }
}

@Composable
private fun EpisodeSelectionSheet(
    episodes: List<PlayableEpisode>,
    selectedEpisodeIndex: Int,
    onSelectEpisode: (Int) -> Unit
) {
    val total = episodes.size
    var selectedRange by remember(total) {
        mutableIntStateOf(episodeRangeIndex(selectedEpisodeIndex, total))
    }
    LaunchedEffect(selectedEpisodeIndex, total) {
        selectedRange = episodeRangeIndex(selectedEpisodeIndex, total)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "选集",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EpisodePicker(
                episodes = episodes.map { episode ->
                    EpisodePickerItem(
                        index = episode.index,
                        title = episode.title,
                        canCast = episode.isHls
                    )
                },
                selectedEpisode = selectedEpisodeIndex,
                selectedRange = selectedRange,
                onSelectRange = { selectedRange = it },
                onSelectEpisode = onSelectEpisode,
                showCastActions = false
            )
        }
    }
}

@Composable
private fun SelectionSheet(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
        ) {
            items(items) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelect(item) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (item == selectedItem) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        color = if (item == selectedItem) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412, heightDp = 520)
@Composable
private fun PlayerSourceSheetPreview() {
    iCinemaTheme {
        SelectionSheet(
            title = "切换线路",
            items = PlayerPreviewData.playSources.map { it.key },
            selectedItem = PlayerPreviewData.state().selectedSourceKey,
            onSelect = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111, widthDp = 412, heightDp = 520)
@Composable
private fun PlayerEpisodeSheetPreview() {
    iCinemaTheme {
        EpisodeSelectionSheet(
            episodes = PlayerPreviewData.episodes,
            selectedEpisodeIndex = 2,
            onSelectEpisode = {}
        )
    }
}
