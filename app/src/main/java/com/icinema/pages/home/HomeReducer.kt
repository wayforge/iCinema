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
                val currentSection = current.discoverStates[mutation.categoryId]
                    ?: HomeContract.VideoSectionState()
                val hasCachedVideos = currentSection.videos.isNotEmpty()
                val nextSection = currentSection.copy(
                    isLoading = mutation.page == 1 && !mutation.isRefresh && !hasCachedVideos,
                    isBackgroundLoading = mutation.page == 1 && !mutation.isRefresh && hasCachedVideos,
                    isRefreshing = mutation.isRefresh,
                    isLoadingMore = mutation.page > 1,
                    error = null,
                    currentPage = if (mutation.page > 1) currentSection.currentPage else 1,
                    hasMorePages = if (mutation.page == 1) true else currentSection.hasMorePages
                )
                current.copy(
                    discoverStates = current.discoverStates + (mutation.categoryId to nextSection),
                    selectedCategoryId = mutation.categoryId
                )
            }

            is HomeContract.Mutation.DiscoverLoadSucceeded -> {
                val currentSection = current.discoverStates[mutation.categoryId]
                    ?: HomeContract.VideoSectionState()
                val mergedVideos = if (mutation.page > 1) {
                    val existingIds = currentSection.videos.map { it.id }.toSet()
                    currentSection.videos + mutation.videos.filter { it.id !in existingIds }
                } else {
                    mutation.videos
                }
                val nextSection = currentSection.copy(
                    isLoading = false,
                    isBackgroundLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    videos = mergedVideos,
                    error = null,
                    currentPage = mutation.page,
                    hasMorePages = mutation.videos.isNotEmpty()
                )
                current.copy(
                    discoverStates = current.discoverStates + (mutation.categoryId to nextSection)
                )
            }

            is HomeContract.Mutation.DiscoverLoadFailed -> {
                val currentSection = current.discoverStates[mutation.categoryId]
                    ?: HomeContract.VideoSectionState()
                val nextSection = currentSection.copy(
                    isLoading = false,
                    isBackgroundLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = mutation.message
                )
                current.copy(
                    discoverStates = current.discoverStates + (mutation.categoryId to nextSection)
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
                        results = current.searchState.results.copy(
                            isLoading = mutation.page == 1 && !mutation.isRefresh,
                            isBackgroundLoading = false,
                            isRefreshing = mutation.isRefresh,
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
                        results = current.searchState.results.copy(
                            isLoading = false,
                            isBackgroundLoading = false,
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
                        results = current.searchState.results.copy(
                            isLoading = false,
                            isBackgroundLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = mutation.message
                        )
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
                val visibleIds = mutation.visibleCategories.map { it.id }.toSet()
                val currentSelected = current.selectedCategoryId
                val nextSelected = if (currentSelected != null && currentSelected !in visibleIds) {
                    null
                } else {
                    currentSelected
                }
                val nextDiscoverStates = buildMap<Int?, HomeContract.VideoSectionState> {
                    current.discoverStates.forEach { (categoryId, section) ->
                        if (categoryId == null || categoryId in visibleIds) {
                            put(categoryId, section)
                        }
                    }
                }
                val resolvedDiscoverStates = if (nextDiscoverStates.isEmpty()) {
                    mapOf<Int?, HomeContract.VideoSectionState>(
                        null to HomeContract.VideoSectionState()
                    )
                } else {
                    nextDiscoverStates
                }
                current.copy(
                    discoverStates = resolvedDiscoverStates,
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
