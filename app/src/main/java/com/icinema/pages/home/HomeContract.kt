package com.icinema.pages.home

import com.icinema.domain.model.Category
import com.icinema.domain.model.Video

object HomeContract {
    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isSearching: Boolean = false,
        val videos: List<Video> = emptyList(),
        val error: String? = null,
        val currentPage: Int = 1,
        val hasMorePages: Boolean = true,
        val isSearchMode: Boolean = false,
        val searchKeyword: String = "",
        val categories: List<Category> = emptyList(),
        val selectedCategoryId: Int? = null
    )

    sealed interface UiIntent {
        data class LoadVideos(
            val page: Int = 1,
            val categoryId: Int? = null,
            val keyword: String? = null,
            val isRefresh: Boolean = false
        ) : UiIntent

        data object LoadMore : UiIntent
        data object Refresh : UiIntent
        data class SelectCategory(val categoryId: Int?) : UiIntent
        data class Search(val keyword: String) : UiIntent
        data object ClearSearch : UiIntent
    }

    sealed interface UiEffect {
        data class ShowError(val message: String) : UiEffect
        data class ShowToast(val message: String) : UiEffect
    }

    sealed interface Mutation {
        data class CategoriesLoaded(val categories: List<Category>) : Mutation
        data class LoadStarted(
            val page: Int,
            val isRefresh: Boolean,
            val isSearchMode: Boolean,
            val keyword: String?,
            val categoryId: Int?
        ) : Mutation

        data class LoadSucceeded(
            val videos: List<Video>,
            val page: Int,
            val isSearchMode: Boolean,
            val keyword: String?,
            val categoryId: Int?
        ) : Mutation

        data class LoadFailed(val message: String) : Mutation
        data class SearchModeChanged(val isSearchMode: Boolean, val keyword: String) : Mutation
        data class CategoryChanged(val categoryId: Int?) : Mutation
    }
}
