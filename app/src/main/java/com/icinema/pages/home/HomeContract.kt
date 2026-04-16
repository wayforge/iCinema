package com.icinema.pages.home

import com.icinema.domain.model.Category
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem

object HomeContract {
    enum class SortMode {
        Latest,
        Name
    }

    data class VideoSectionState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val videos: List<Video> = emptyList(),
        val error: String? = null,
        val currentPage: Int = 1,
        val hasMorePages: Boolean = true
    )

    data class SearchSectionState(
        val input: String = "",
        val query: String = "",
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false,
        val results: VideoSectionState = VideoSectionState(),
        val shouldShowRefreshIndicator: Boolean = false
    )

    data class UiState(
        val discoverState: VideoSectionState = VideoSectionState(),
        val searchState: SearchSectionState = SearchSectionState(),
        val categories: List<Category> = emptyList(),
        val visibleCategories: List<Category> = emptyList(),
        val selectedCategoryId: Int? = null,
        val selectedCategoryIds: Set<Int> = emptySet(),
        val continueWatching: List<WatchHistoryItem> = emptyList(),
        val historyCount: Int = 0,
        val searchHistory: List<String> = emptyList(),
        val hotKeywords: List<String> = emptyList(),
        val recommendedVideos: List<Video> = emptyList(),
        val sortMode: SortMode = SortMode.Latest
    )

    sealed interface UiIntent {
        data class LoadDiscoverVideos(
            val page: Int = 1,
            val categoryId: Int? = null,
            val isRefresh: Boolean = false
        ) : UiIntent

        data object LoadMoreDiscover : UiIntent
        data object RefreshDiscover : UiIntent
        data object RestoreDiscover : UiIntent
        data class SelectCategory(val categoryId: Int?) : UiIntent
        data class Search(val keyword: String) : UiIntent
        data object LoadMoreSearch : UiIntent
        data object RefreshSearch : UiIntent
        data object RestoreSearch : UiIntent
        data object ClearSearch : UiIntent
        data object LoadContinueWatching : UiIntent
        data object LoadSearchSuggestions : UiIntent
        data class QuickSearch(val keyword: String) : UiIntent
        data object ClearSearchHistory : UiIntent
        data object LoadRecommendations : UiIntent
        data class ChangeSort(val sortMode: SortMode) : UiIntent
        data class OpenVideoDetail(val videoId: Long) : UiIntent
        data class OpenContinueWatching(
            val videoId: Long,
            val sourceKey: String,
            val episodeIndex: Int
        ) : UiIntent
    }

    sealed interface UiEffect {
        data class ShowError(val message: String) : UiEffect
        data class ShowToast(val message: String) : UiEffect
        data class OpenDetail(val videoId: Long) : UiEffect
        data class OpenPlayer(
            val videoId: Long,
            val sourceKey: String,
            val episodeIndex: Int
        ) : UiEffect
    }

    sealed interface Mutation {
        data class CategoriesLoaded(val categories: List<Category>) : Mutation
        data class DiscoverLoadStarted(
            val page: Int,
            val isRefresh: Boolean,
            val categoryId: Int?
        ) : Mutation

        data class DiscoverLoadSucceeded(
            val videos: List<Video>,
            val page: Int,
            val categoryId: Int?
        ) : Mutation

        data class DiscoverLoadFailed(val message: String) : Mutation
        data class SearchInputChanged(val input: String) : Mutation
        data class SearchLoadStarted(
            val page: Int,
            val query: String,
            val isRefresh: Boolean = false,
            val showRefreshIndicator: Boolean = false
        ) : Mutation

        data class SearchLoadSucceeded(
            val videos: List<Video>,
            val page: Int,
            val query: String
        ) : Mutation

        data class SearchLoadFailed(val message: String) : Mutation
        data class SearchRefreshIndicatorChanged(val visible: Boolean) : Mutation
        data object SearchCleared : Mutation
        data class CategoryChanged(val categoryId: Int?) : Mutation
        data class VisibleCategoriesUpdated(
            val visibleCategories: List<Category>,
            val selectedCategoryIds: Set<Int>
        ) : Mutation

        data class ContinueWatchingLoaded(
            val items: List<WatchHistoryItem>
        ) : Mutation

        data class SearchSuggestionsLoaded(
            val history: List<String>,
            val hotKeywords: List<String>
        ) : Mutation

        data class RecommendationsLoaded(
            val videos: List<Video>
        ) : Mutation

        data class SortChanged(val sortMode: SortMode) : Mutation
    }
}
