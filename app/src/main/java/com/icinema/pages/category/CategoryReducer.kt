package com.icinema.pages.category

import javax.inject.Inject

class CategoryReducer @Inject constructor() {
    fun reduce(
        current: CategoryContract.UiState,
        mutation: CategoryContract.Mutation
    ): CategoryContract.UiState {
        return when (mutation) {
            is CategoryContract.Mutation.LoadStarted -> {
                current.copy(isLoading = mutation.loading, error = null)
            }

            is CategoryContract.Mutation.CategoriesLoaded -> {
                current.copy(
                    categories = mutation.categories,
                    editingCategoryIds = mutation.editingCategoryIds,
                    isLoading = false,
                    error = null
                )
            }

            is CategoryContract.Mutation.CategoryToggled -> {
                current.copy(editingCategoryIds = mutation.categoryIds)
            }

            is CategoryContract.Mutation.LoadFailed -> {
                current.copy(isLoading = false, error = mutation.message)
            }
        }
    }
}
