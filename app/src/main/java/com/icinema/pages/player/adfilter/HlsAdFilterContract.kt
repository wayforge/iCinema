package com.icinema.pages.player.adfilter

object HlsAdFilterContract {
    data class UiState(
        val isLoading: Boolean = false,
        val rules: List<AdRuleItem> = emptyList()
    )

    data class AdRuleItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val segmentUrl: String,
        val matchText: String,
        val matchType: String,
        val createdAtText: String
    )

    sealed interface UiIntent {
        data object Load : UiIntent
        data class DeleteRule(val ruleId: String) : UiIntent
        data object ClearAll : UiIntent
        data class PreviewRule(val ruleId: String) : UiIntent
    }

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class OpenPreview(val segmentUrl: String, val title: String) : UiEffect
    }
}
