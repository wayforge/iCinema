package com.icinema.pages.favorite

import com.icinema.domain.model.FavoriteItem

object FavoriteContract {
    data class UiState(
        val isLoading: Boolean = false,
        val items: List<FavoriteItem> = emptyList(),
        val error: String? = null
    )

    sealed interface UiIntent {
        data object Load : UiIntent
        data class OpenDetail(val videoId: Long) : UiIntent
    }

    sealed interface UiEffect {
        data class OpenDetail(val videoId: Long) : UiEffect
    }

    sealed interface Mutation {
        data object LoadStarted : Mutation
        data class LoadSucceeded(val items: List<FavoriteItem>) : Mutation
        data class LoadFailed(val message: String) : Mutation
    }
}
