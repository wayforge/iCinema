package com.icinema.pages.home

import javax.inject.Inject

class HomeReducer @Inject constructor() {
    fun reduce(
        current: HomeContract.UiState,
        mutation: HomeContract.Mutation
    ): HomeContract.UiState {
        return when (mutation) {
            is HomeContract.Mutation.CategoriesLoaded -> {
                current.copy(categories = mutation.categories)
            }

            is HomeContract.Mutation.LoadStarted -> {
                current.copy(
                    isLoading = mutation.page == 1 && !mutation.isRefresh,
                    isRefreshing = mutation.isRefresh,
                    isLoadingMore = mutation.page > 1,
                    isSearching = mutation.isSearchMode,
                    error = null,
                    isSearchMode = mutation.isSearchMode,
                    searchKeyword = mutation.keyword ?: "",
                    selectedCategoryId = if (mutation.isSearchMode) null else mutation.categoryId
                )
            }

            is HomeContract.Mutation.LoadSucceeded -> {
                val mergedVideos = if (mutation.page > 1) {
                    val existingIds = current.videos.map { it.id }.toSet()
                    current.videos + mutation.videos.filter { it.id !in existingIds }
                } else {
                    mutation.videos
                }
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isSearching = false,
                    videos = mergedVideos,
                    error = null,
                    currentPage = mutation.page,
                    hasMorePages = mutation.videos.isNotEmpty(),
                    isSearchMode = mutation.isSearchMode,
                    searchKeyword = mutation.keyword ?: "",
                    selectedCategoryId = if (mutation.isSearchMode) null else mutation.categoryId
                )
            }

            is HomeContract.Mutation.LoadFailed -> {
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isSearching = false,
                    error = mutation.message
                )
            }

            is HomeContract.Mutation.SearchModeChanged -> {
                current.copy(
                    isSearchMode = mutation.isSearchMode,
                    searchKeyword = mutation.keyword,
                    selectedCategoryId = if (mutation.isSearchMode) null else current.selectedCategoryId
                )
            }

            is HomeContract.Mutation.CategoryChanged -> {
                current.copy(
                    selectedCategoryId = mutation.categoryId,
                    isSearchMode = false,
                    searchKeyword = ""
                )
            }
        }
    }
}
