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

            is HomeContract.Mutation.DiscoverLoadStarted -> {
                current.copy(
                    discoverState = current.discoverState.copy(
                        isLoading = mutation.page == 1 && !mutation.isRefresh,
                        isRefreshing = mutation.isRefresh,
                        isLoadingMore = mutation.page > 1,
                        error = null,
                        currentPage = if (mutation.page > 1) current.discoverState.currentPage else 1,
                        hasMorePages = if (mutation.page == 1) true else current.discoverState.hasMorePages
                    ),
                    selectedCategoryId = mutation.categoryId
                )
            }

            is HomeContract.Mutation.DiscoverLoadSucceeded -> {
                val mergedVideos = if (mutation.page > 1) {
                    val existingIds = current.discoverState.videos.map { it.id }.toSet()
                    current.discoverState.videos + mutation.videos.filter { it.id !in existingIds }
                } else {
                    mutation.videos
                }
                current.copy(
                    discoverState = current.discoverState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        videos = mergedVideos,
                        error = null,
                        currentPage = mutation.page,
                        hasMorePages = mutation.videos.isNotEmpty()
                    ),
                    selectedCategoryId = mutation.categoryId
                )
            }

            is HomeContract.Mutation.DiscoverLoadFailed -> {
                current.copy(
                    discoverState = current.discoverState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = mutation.message
                    )
                )
            }

            is HomeContract.Mutation.SearchInputChanged -> {
                current.copy(
                    searchState = current.searchState.copy(input = mutation.input)
                )
            }

            is HomeContract.Mutation.SearchLoadStarted -> {
                current.copy(
                    searchState = current.searchState.copy(
                        input = mutation.query,
                        query = mutation.query,
                        isSearching = true,
                        hasSearched = mutation.query.isNotBlank(),
                        shouldShowRefreshIndicator = mutation.showRefreshIndicator,
                        results = current.searchState.results.copy(
                            isLoading = mutation.page == 1 && !mutation.isRefresh,
                            isRefreshing = mutation.isRefresh || mutation.showRefreshIndicator,
                            isLoadingMore = mutation.page > 1,
                            error = null,
                            currentPage = if (mutation.page > 1) current.searchState.results.currentPage else 1,
                            hasMorePages = if (mutation.page == 1) true else current.searchState.results.hasMorePages
                        )
                    )
                )
            }

            is HomeContract.Mutation.SearchLoadSucceeded -> {
                val mergedVideos = if (mutation.page > 1) {
                    val existingIds = current.searchState.results.videos.map { it.id }.toSet()
                    current.searchState.results.videos + mutation.videos.filter { it.id !in existingIds }
                } else {
                    mutation.videos
                }
                current.copy(
                    searchState = current.searchState.copy(
                        input = mutation.query,
                        query = mutation.query,
                        isSearching = false,
                        hasSearched = mutation.query.isNotBlank(),
                        shouldShowRefreshIndicator = false,
                        results = current.searchState.results.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            videos = mergedVideos,
                            error = null,
                            currentPage = mutation.page,
                            hasMorePages = mutation.videos.isNotEmpty()
                        )
                    )
                )
            }

            is HomeContract.Mutation.SearchLoadFailed -> {
                current.copy(
                    searchState = current.searchState.copy(
                        isSearching = false,
                        shouldShowRefreshIndicator = false,
                        results = current.searchState.results.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = mutation.message
                        )
                    )
                )
            }

            is HomeContract.Mutation.SearchRefreshIndicatorChanged -> {
                current.copy(
                    searchState = current.searchState.copy(
                        shouldShowRefreshIndicator = mutation.visible,
                        results = current.searchState.results.copy(isRefreshing = mutation.visible)
                    )
                )
            }

            HomeContract.Mutation.SearchCleared -> {
                current.copy(searchState = HomeContract.SearchSectionState())
            }

            is HomeContract.Mutation.CategoryChanged -> {
                current.copy(selectedCategoryId = mutation.categoryId)
            }

            is HomeContract.Mutation.VisibleCategoriesUpdated -> {
                val currentSelected = current.selectedCategoryId
                val nextSelected = if (currentSelected != null && mutation.visibleCategories.none { it.id == currentSelected }) {
                    null
                } else {
                    currentSelected
                }
                current.copy(
                    visibleCategories = mutation.visibleCategories,
                    selectedCategoryIds = mutation.selectedCategoryIds,
                    selectedCategoryId = nextSelected
                )
            }

            is HomeContract.Mutation.ContinueWatchingLoaded -> {
                current.copy(
                    continueWatching = mutation.items,
                    historyCount = mutation.items.size
                )
            }

            is HomeContract.Mutation.SearchSuggestionsLoaded -> {
                current.copy(
                    searchHistory = mutation.history,
                    hotKeywords = mutation.hotKeywords
                )
            }

            is HomeContract.Mutation.RecommendationsLoaded -> {
                current.copy(recommendedVideos = mutation.videos)
            }

            is HomeContract.Mutation.SortChanged -> {
                current.copy(sortMode = mutation.sortMode)
            }
        }
    }
}
