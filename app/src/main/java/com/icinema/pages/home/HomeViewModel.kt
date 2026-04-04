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
        handleIntent(HomeContract.UiIntent.LoadVideos())
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
            handleIntent(HomeContract.UiIntent.LoadVideos(page = 1, categoryId = null))
        }
    }

    private fun loadVideos(
        page: Int = 1,
        categoryId: Int? = null,
        keyword: String? = null,
        isRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            val isSearchMode = !keyword.isNullOrBlank()
            commit(
                HomeContract.Mutation.LoadStarted(
                    page = page,
                    isRefresh = isRefresh,
                    isSearchMode = isSearchMode,
                    keyword = keyword,
                    categoryId = categoryId
                )
            )

            val result = if (isSearchMode) {
                bizPort.searchVideos(keyword = keyword.orEmpty(), page = page)
            } else {
                bizPort.loadVideos(page = page, categoryId = categoryId, keyword = keyword)
            }

            result
                .onSuccess { videos ->
                    commit(
                        HomeContract.Mutation.LoadSucceeded(
                            videos = videos,
                            page = page,
                            isSearchMode = isSearchMode,
                            keyword = keyword,
                            categoryId = categoryId
                        )
                    )
                }
                .onFailure { exception ->
                    val message = exception.message ?: "Unknown error"
                    commit(HomeContract.Mutation.LoadFailed(message))
                    emitEffect(HomeContract.UiEffect.ShowError(message))
                }
        }
    }

    fun handleIntent(intent: HomeContract.UiIntent) {
        when (intent) {
            is HomeContract.UiIntent.LoadVideos -> loadVideos(
                page = intent.page,
                categoryId = intent.categoryId,
                keyword = intent.keyword,
                isRefresh = intent.isRefresh
            )
            HomeContract.UiIntent.LoadMore -> loadMore()
            HomeContract.UiIntent.Refresh -> refresh()
            is HomeContract.UiIntent.SelectCategory -> selectCategory(intent.categoryId)
            is HomeContract.UiIntent.Search -> search(intent.keyword)
            HomeContract.UiIntent.ClearSearch -> clearSearch()
        }
    }

    private fun loadMore() {
        val currentState = _uiState.value
        if (!currentState.isLoading && !currentState.isLoadingMore && currentState.hasMorePages) {
            handleIntent(
                HomeContract.UiIntent.LoadVideos(
                    page = currentState.currentPage + 1,
                    categoryId = currentState.selectedCategoryId,
                    keyword = if (currentState.isSearchMode) currentState.searchKeyword else null
                )
            )
        }
    }

    private fun refresh() {
        handleIntent(
            HomeContract.UiIntent.LoadVideos(
                page = 1,
                categoryId = _uiState.value.selectedCategoryId,
                keyword = if (_uiState.value.isSearchMode) _uiState.value.searchKeyword else null,
                isRefresh = true
            )
        )
    }

    private fun selectCategory(categoryId: Int?) {
        if (_uiState.value.selectedCategoryId != categoryId) {
            commit(HomeContract.Mutation.CategoryChanged(categoryId))
            handleIntent(HomeContract.UiIntent.LoadVideos(page = 1, categoryId = categoryId))
        }
    }

    private fun search(keyword: String) {
        viewModelScope.launch {
            commit(
                HomeContract.Mutation.LoadStarted(
                    page = 1,
                    isRefresh = false,
                    isSearchMode = true,
                    keyword = keyword,
                    categoryId = null
                )
            )

            bizPort.searchVideos(keyword, page = 1)
                .onSuccess { videos ->
                    commit(
                        HomeContract.Mutation.LoadSucceeded(
                            videos = videos,
                            page = 1,
                            isSearchMode = true,
                            keyword = keyword,
                            categoryId = null
                        )
                    )
                }
                .onFailure { exception ->
                    val message = exception.message ?: "搜索失败"
                    commit(HomeContract.Mutation.LoadFailed(message))
                    emitEffect(HomeContract.UiEffect.ShowError(message))
                }
        }
    }

    private fun clearSearch() {
        val categoryId = _uiState.value.selectedCategoryId
        commit(HomeContract.Mutation.SearchModeChanged(isSearchMode = false, keyword = ""))
        handleIntent(HomeContract.UiIntent.LoadVideos(page = 1, categoryId = categoryId))
    }


    private fun commit(mutation: HomeContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: HomeContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
