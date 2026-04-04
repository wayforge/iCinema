package com.icinema.pages.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class CategoryViewModel @Inject constructor(
    private val bizPort: CategoryBizPort,
    private val reducer: CategoryReducer,
    private val selectionStore: CategorySelectionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryContract.UiState())
    val uiState: StateFlow<CategoryContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<CategoryContract.UiEffect>()
    val uiEffect: Flow<CategoryContract.UiEffect> = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: CategoryContract.UiIntent) {
        when (intent) {
            CategoryContract.UiIntent.Load -> loadCategories()
            is CategoryContract.UiIntent.ToggleCategory -> toggleCategory(intent.categoryId)
            CategoryContract.UiIntent.ToggleSelectAll -> toggleSelectAll()
            CategoryContract.UiIntent.SaveAndExit -> saveAndExit()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            commit(CategoryContract.Mutation.LoadStarted(true))
            bizPort.loadCategories()
                .onSuccess { categories ->
                    val allIds = categories.map { it.id }.toSet()
                    val persisted = if (selectionStore.hasSavedSelection()) {
                        selectionStore.loadSelectedCategoryIds()
                    } else {
                        allIds
                    }
                    val resolved = selectionStore.resolveVisibleCategoryIds(persisted, allIds)
                    if (persisted != resolved || !selectionStore.hasSavedSelection()) {
                        selectionStore.saveSelectedCategoryIds(resolved)
                    }
                    commit(CategoryContract.Mutation.CategoriesLoaded(categories, resolved))
                }
                .onFailure { error ->
                    val message = error.message ?: "分类加载失败"
                    commit(CategoryContract.Mutation.LoadFailed(message))
                    emitEffect(CategoryContract.UiEffect.ShowMessage(message))
                }
        }
    }

    private fun toggleCategory(categoryId: Int) {
        val current = _uiState.value.editingCategoryIds
        val next = if (categoryId in current) current - categoryId else current + categoryId
        commit(CategoryContract.Mutation.CategoryToggled(next))
    }

    private fun toggleSelectAll() {
        val allIds = _uiState.value.categories.map { it.id }.toSet()
        val current = _uiState.value.editingCategoryIds
        val next = if (current.size == allIds.size && allIds.isNotEmpty()) {
            emptySet()
        } else {
            allIds
        }
        commit(CategoryContract.Mutation.CategoryToggled(next))
    }

    private fun saveAndExit() {
        val state = _uiState.value
        val allIds = state.categories.map { it.id }.toSet()
        val previous = selectionStore.loadSelectedCategoryIds()
        val resolved = selectionStore.resolveVisibleCategoryIds(state.editingCategoryIds, allIds)
        selectionStore.saveSelectedCategoryIds(resolved)
        emitEffect(CategoryContract.UiEffect.FinishWithResult(previous != resolved))
    }

    private fun commit(mutation: CategoryContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: CategoryContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
