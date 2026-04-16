package com.icinema.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icinema.domain.model.Category
import com.icinema.pages.category.CategorySelectionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bizPort: HomeBizPort,
    private val reducer: HomeReducer,
    private val categorySelectionStore: CategorySelectionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeContract.UiState())
    val uiState: StateFlow<HomeContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeContract.UiEffect>()
    val uiEffect: Flow<HomeContract.UiEffect> = _uiEffect.receiveAsFlow()

    init {
        loadCategories()
        handleIntent(HomeContract.UiIntent.LoadDiscoverVideos())
        handleIntent(HomeContract.UiIntent.LoadContinueWatching)
        handleIntent(HomeContract.UiIntent.LoadSearchSuggestions)
        handleIntent(HomeContract.UiIntent.LoadRecommendations)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            bizPort.loadCategories()
                .onSuccess { categories ->
                    commit(HomeContract.Mutation.CategoriesLoaded(categories))
                    applyCategoryVisibility(categories)
                }
                .onFailure { exception ->
                    emitEffect(HomeContract.UiEffect.ShowToast(exception.message ?: "分类加载失败"))
                }
        }
    }

    fun reloadCategories() {
        loadCategories()
    }

    fun refreshPlaybackDrivenSections() {
        handleIntent(HomeContract.UiIntent.LoadContinueWatching)
        handleIntent(HomeContract.UiIntent.LoadRecommendations)
    }

    fun refreshMineTab() {
        refreshPlaybackDrivenSections()
    }

    private fun applyCategoryVisibility(categories: List<Category>) {
        val allCategoryIds = categories.map { it.id }.toSet()
        val persisted = if (categorySelectionStore.hasSavedSelection()) {
            categorySelectionStore.loadSelectedCategoryIds()
        } else {
            allCategoryIds
        }
        val resolved = categorySelectionStore.resolveVisibleCategoryIds(persisted, allCategoryIds)
        if (persisted != resolved || !categorySelectionStore.hasSavedSelection()) {
            categorySelectionStore.saveSelectedCategoryIds(resolved)
        }
        val visible = categories.filter { it.id in resolved }
        commit(HomeContract.Mutation.VisibleCategoriesUpdated(visible, resolved))

        val currentSelected = _uiState.value.selectedCategoryId
        if (currentSelected != null && currentSelected !in resolved) {
            commit(HomeContract.Mutation.CategoryChanged(null))
            handleIntent(HomeContract.UiIntent.LoadDiscoverVideos(page = 1, categoryId = null))
        }
    }

    private fun loadDiscoverVideos(
        page: Int = 1,
        categoryId: Int? = null,
        isRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            commit(
                HomeContract.Mutation.DiscoverLoadStarted(
                    page = page,
                    isRefresh = isRefresh,
                    categoryId = categoryId
                )
            )

            bizPort.loadVideos(page = page, categoryId = categoryId, keyword = null)
                .onSuccess { videos ->
                    commit(
                        HomeContract.Mutation.DiscoverLoadSucceeded(
                            videos = videos,
                            page = page,
                            categoryId = categoryId
                        )
                    )
                }
                .onFailure { exception ->
                    val message = exception.message ?: "Unknown error"
                    commit(HomeContract.Mutation.DiscoverLoadFailed(message))
                    emitEffect(HomeContract.UiEffect.ShowError(message))
                }
        }
    }

    private fun searchVideos(
        keyword: String,
        page: Int = 1,
        isRefresh: Boolean = false,
        showRefreshIndicator: Boolean = false
    ) {
        viewModelScope.launch {
            val normalized = keyword.trim()
            commit(
                HomeContract.Mutation.SearchLoadStarted(
                    page = page,
                    query = normalized,
                    isRefresh = isRefresh,
                    showRefreshIndicator = showRefreshIndicator
                )
            )

            if (page == 1 && !isRefresh && !showRefreshIndicator) {
                bizPort.saveSearchKeyword(normalized)
            }

            bizPort.searchVideos(normalized, page = page)
                .onSuccess { videos ->
                    commit(
                        HomeContract.Mutation.SearchLoadSucceeded(
                            videos = videos,
                            page = page,
                            query = normalized
                        )
                    )
                    if (page == 1) {
                        loadSearchSuggestions()
                    }
                }
                .onFailure { exception ->
                    val message = exception.message ?: "搜索失败"
                    commit(HomeContract.Mutation.SearchLoadFailed(message))
                    emitEffect(HomeContract.UiEffect.ShowError(message))
                }
        }
    }

    fun handleIntent(intent: HomeContract.UiIntent) {
        when (intent) {
            is HomeContract.UiIntent.LoadDiscoverVideos -> loadDiscoverVideos(
                page = intent.page,
                categoryId = intent.categoryId,
                isRefresh = intent.isRefresh
            )
            HomeContract.UiIntent.LoadMoreDiscover -> loadMoreDiscover()
            HomeContract.UiIntent.RefreshDiscover -> refreshDiscover()
            HomeContract.UiIntent.RestoreDiscover -> restoreDiscover()
            is HomeContract.UiIntent.SelectCategory -> selectCategory(intent.categoryId)
            is HomeContract.UiIntent.Search -> search(intent.keyword)
            HomeContract.UiIntent.LoadMoreSearch -> loadMoreSearch()
            HomeContract.UiIntent.RefreshSearch -> refreshSearch()
            HomeContract.UiIntent.RestoreSearch -> restoreSearch()
            HomeContract.UiIntent.ClearSearch -> clearSearch()
            HomeContract.UiIntent.LoadContinueWatching -> loadContinueWatching()
            HomeContract.UiIntent.LoadSearchSuggestions -> loadSearchSuggestions()
            is HomeContract.UiIntent.QuickSearch -> search(intent.keyword)
            HomeContract.UiIntent.ClearSearchHistory -> clearSearchHistory()
            HomeContract.UiIntent.LoadRecommendations -> loadRecommendations()
            is HomeContract.UiIntent.ChangeSort -> changeSort(intent.sortMode)
            is HomeContract.UiIntent.OpenVideoDetail -> openVideoDetail(intent.videoId)
            is HomeContract.UiIntent.OpenContinueWatching -> openContinueWatching(
                videoId = intent.videoId,
                sourceKey = intent.sourceKey,
                episodeIndex = intent.episodeIndex
            )
        }
    }

    fun updateSearchInput(input: String) {
        commit(HomeContract.Mutation.SearchInputChanged(input))
    }

    private fun loadMoreDiscover() {
        val currentState = _uiState.value.discoverState
        if (!currentState.isLoading && !currentState.isLoadingMore && currentState.hasMorePages) {
            handleIntent(
                HomeContract.UiIntent.LoadDiscoverVideos(
                    page = currentState.currentPage + 1,
                    categoryId = _uiState.value.selectedCategoryId
                )
            )
        }
    }

    private fun refreshDiscover() {
        handleIntent(
            HomeContract.UiIntent.LoadDiscoverVideos(
                page = 1,
                categoryId = _uiState.value.selectedCategoryId,
                isRefresh = true
            )
        )
    }

    private fun restoreDiscover() {
        val discoverState = _uiState.value.discoverState
        if (discoverState.videos.isEmpty() && !discoverState.isLoading) {
            handleIntent(
                HomeContract.UiIntent.LoadDiscoverVideos(
                    page = 1,
                    categoryId = _uiState.value.selectedCategoryId
                )
            )
        } else if (discoverState.videos.isNotEmpty() && !discoverState.isRefreshing && !discoverState.isLoadingMore) {
            handleIntent(HomeContract.UiIntent.RefreshDiscover)
        }
    }

    private fun selectCategory(categoryId: Int?) {
        if (_uiState.value.selectedCategoryId != categoryId) {
            commit(HomeContract.Mutation.CategoryChanged(categoryId))
            handleIntent(HomeContract.UiIntent.LoadDiscoverVideos(page = 1, categoryId = categoryId))
        }
    }

    private fun search(keyword: String) {
        if (keyword.isBlank()) {
            clearSearch()
            return
        }
        searchVideos(keyword = keyword, page = 1)
    }

    private fun loadMoreSearch() {
        val currentState = _uiState.value.searchState
        val results = currentState.results
        if (currentState.query.isBlank()) return
        if (!results.isLoading && !results.isLoadingMore && !results.isRefreshing && results.hasMorePages) {
            searchVideos(keyword = currentState.query, page = results.currentPage + 1)
        }
    }

    private fun refreshSearch(showRefreshIndicator: Boolean = false) {
        val currentState = _uiState.value.searchState
        if (currentState.query.isBlank()) return
        searchVideos(
            keyword = currentState.query,
            page = 1,
            isRefresh = true,
            showRefreshIndicator = showRefreshIndicator
        )
    }

    private fun restoreSearch() {
        val currentState = _uiState.value.searchState
        val results = currentState.results
        if (currentState.query.isBlank()) return
        if (results.videos.isEmpty() && !results.isLoading) {
            searchVideos(keyword = currentState.query, page = 1)
        } else if (results.videos.isNotEmpty() && !results.isRefreshing && !results.isLoadingMore) {
            refreshSearch(showRefreshIndicator = true)
        }
    }

    private fun clearSearch() {
        commit(HomeContract.Mutation.SearchCleared)
        loadSearchSuggestions()
    }

    private fun loadContinueWatching() {
        viewModelScope.launch {
            bizPort.loadContinueWatching(limit = 10)
                .onSuccess { items ->
                    commit(HomeContract.Mutation.ContinueWatchingLoaded(items))
                }
                .onFailure {
                    commit(HomeContract.Mutation.ContinueWatchingLoaded(emptyList()))
                }
        }
    }

    private fun loadSearchSuggestions() {
        viewModelScope.launch {
            val history = bizPort.loadSearchHistory(limit = 20).getOrDefault(emptyList())
            val hotKeywords = bizPort.loadHotKeywords().getOrDefault(emptyList())
            commit(HomeContract.Mutation.SearchSuggestionsLoaded(history, hotKeywords))
        }
    }

    private fun clearSearchHistory() {
        viewModelScope.launch {
            bizPort.clearSearchHistory()
            loadSearchSuggestions()
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            val recommended = bizPort.loadRecommendations(limit = 10).getOrDefault(emptyList())
            val sorted = applySort(recommended, _uiState.value.sortMode)
            commit(HomeContract.Mutation.RecommendationsLoaded(sorted))
        }
    }

    private fun changeSort(sortMode: HomeContract.SortMode) {
        commit(HomeContract.Mutation.SortChanged(sortMode))
        val sorted = applySort(_uiState.value.recommendedVideos, sortMode)
        commit(HomeContract.Mutation.RecommendationsLoaded(sorted))
    }

    private fun openVideoDetail(videoId: Long) {
        emitEffect(HomeContract.UiEffect.OpenDetail(videoId))
    }

    private fun openContinueWatching(videoId: Long, sourceKey: String, episodeIndex: Int) {
        emitEffect(
            HomeContract.UiEffect.OpenPlayer(
                videoId = videoId,
                sourceKey = sourceKey,
                episodeIndex = episodeIndex
            )
        )
    }

    private fun applySort(videos: List<com.icinema.domain.model.Video>, sortMode: HomeContract.SortMode): List<com.icinema.domain.model.Video> {
        return when (sortMode) {
            HomeContract.SortMode.Latest -> videos.sortedWith(
                compareByDescending<com.icinema.domain.model.Video> { it.id }
                    .thenBy { it.name }
            )
            HomeContract.SortMode.Name -> videos.sortedWith(
                compareBy<com.icinema.domain.model.Video> { it.name.lowercase() }
                    .thenByDescending { it.id }
            )
        }
    }

    private fun commit(mutation: HomeContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: HomeContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
