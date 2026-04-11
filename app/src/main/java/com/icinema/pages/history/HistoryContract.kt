package com.icinema.pages.history

import com.icinema.domain.model.WatchHistoryItem

object HistoryContract {
    data class UiState(
        val isLoading: Boolean = false,
        val items: List<WatchHistoryItem> = emptyList(),
        val error: String? = null
    )

    sealed interface UiIntent {
        data object Load : UiIntent
        data class DeleteItem(val id: Long) : UiIntent
        data object ClearAll : UiIntent
        data class OpenDetail(val videoId: Long) : UiIntent
    }

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class OpenDetail(val videoId: Long) : UiEffect
    }

    sealed interface Mutation {
        data object LoadStarted : Mutation
        data class LoadSucceeded(val items: List<WatchHistoryItem>) : Mutation
        data class LoadFailed(val message: String) : Mutation
    }
}
