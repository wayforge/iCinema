package com.icinema.pages.category

import com.icinema.domain.model.Category

object CategoryContract {
    data class UiState(
        val categories: List<Category> = emptyList(),
        val editingCategoryIds: Set<Int> = emptySet(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed interface UiIntent {
        data object Load : UiIntent
        data class ToggleCategory(val categoryId: Int) : UiIntent
        data object ToggleSelectAll : UiIntent
        data object SaveAndExit : UiIntent
    }

    sealed interface UiEffect {
        data class FinishWithResult(val changed: Boolean) : UiEffect
        data class ShowMessage(val message: String) : UiEffect
    }

    sealed interface Mutation {
        data class LoadStarted(val loading: Boolean) : Mutation
        data class CategoriesLoaded(
            val categories: List<Category>,
            val editingCategoryIds: Set<Int>
        ) : Mutation
        data class CategoryToggled(val categoryIds: Set<Int>) : Mutation
        data class LoadFailed(val message: String) : Mutation
    }
}
