package com.icinema.pages.player.adfilter

object HlsAdFilterContract {
    data class UiState(
        val isLoading: Boolean = false,
        val rules: List<AdRuleItem> = emptyList(),
        val detectedSegments: List<DetectedSegmentItem> = emptyList()
    )

    data class AdRuleItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val playlistUrl: String,
        val segmentUrl: String,
        val urlPattern: String?,
        val matchText: String,
        val matchType: String,
        val createdAtText: String,
        val hitCount: Long,
        val lastHitAtText: String?
    )

    data class DetectedSegmentItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val segmentUrl: String,
        val matchText: String,
        val timeRangeText: String,
        val detectedBy: String,
        val detectedCount: Long,
        val lastDetectedAtText: String
    )

    sealed interface UiIntent {
        data object Load : UiIntent
        data class DeleteRule(val ruleId: String) : UiIntent
        data object ClearAll : UiIntent
        data class PreviewRule(val ruleId: String) : UiIntent
        data class PreviewDetectedSegment(val segmentId: String) : UiIntent
        data class SaveRule(
            val ruleId: String?,
            val playlistUrl: String,
            val segmentUrl: String,
            val urlPattern: String?
        ) : UiIntent
        data class ValidateRule(
            val ruleId: String,
            val playlistUrl: String,
            val segmentUrl: String
        ) : UiIntent
    }

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class OpenPreview(val segmentUrl: String, val title: String) : UiEffect
    }
}
