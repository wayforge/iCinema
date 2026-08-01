package com.icinema.pages.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icinema.domain.model.Video
import com.icinema.pages.cleanHtmlContent

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailDescriptionSection(
    video: Video,
    description: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionTitle("简介")

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            video.director?.takeIf { it.isNotBlank() }?.let { value ->
                InfoLine(label = "导演", value = value, modifier = Modifier.weight(1f))
            }
            video.actor?.takeIf { it.isNotBlank() }?.let { value ->
                InfoLine(label = "演员", value = value, modifier = Modifier.weight(1f))
            }
        }

        Text(
            text = description?.cleanHtmlContent().orEmpty().ifBlank { "暂无简介信息。" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
