package com.icinema.pages.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icinema.domain.model.PlaySource
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
    onOpenEpisodes: () -> Unit
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
                SelectionSheet(
                    title = "选集",
                    items = selectedSource?.episodes?.map { it.title }.orEmpty(),
                    selectedItem = selectedSource?.episodes
                        ?.getOrNull(state.selectedEpisodeIndex)
                        ?.title,
                    onSelect = { title ->
                        val index = selectedSource?.episodes?.indexOfFirst { it.title == title } ?: -1
                        if (index >= 0) onSelectEpisode(index)
                    }
                )
            }

            PlayerContract.SheetMode.Details -> {
                PlayerDetailsSection(
                    state = state,
                    selectedSource = selectedSource,
                    onOpenSources = onOpenSources,
                    onOpenEpisodes = onOpenEpisodes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
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
                .height(360.dp)
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
        SelectionSheet(
            title = "选集",
            items = PlayerPreviewData.episodes.map { it.title },
            selectedItem = PlayerPreviewData.state().currentEpisode?.title,
            onSelect = {}
        )
    }
}
