package com.icinema.pages.history

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
class HistoryViewModel @Inject constructor(
    private val bizPort: HistoryBizPort,
    private val reducer: HistoryReducer
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryContract.UiState())
    val uiState: StateFlow<HistoryContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HistoryContract.UiEffect>()
    val uiEffect: Flow<HistoryContract.UiEffect> = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: HistoryContract.UiIntent) {
        when (intent) {
            HistoryContract.UiIntent.Load -> load()
            is HistoryContract.UiIntent.DeleteItem -> deleteItem(intent.id)
            HistoryContract.UiIntent.ClearAll -> clearAll()
            is HistoryContract.UiIntent.OpenDetail -> openDetail(intent.videoId)
        }
    }

    private fun load() {
        viewModelScope.launch {
            commit(HistoryContract.Mutation.LoadStarted)
            bizPort.loadHistory()
                .onSuccess { commit(HistoryContract.Mutation.LoadSucceeded(it)) }
                .onFailure { commit(HistoryContract.Mutation.LoadFailed(it.message ?: "历史加载失败")) }
        }
    }

    private fun deleteItem(id: Long) {
        viewModelScope.launch {
            bizPort.deleteItem(id)
                .onSuccess {
                    emitEffect(HistoryContract.UiEffect.ShowMessage("已删除"))
                    load()
                }
                .onFailure { emitEffect(HistoryContract.UiEffect.ShowMessage(it.message ?: "删除失败")) }
        }
    }

    private fun clearAll() {
        viewModelScope.launch {
            bizPort.clearAll()
                .onSuccess {
                    emitEffect(HistoryContract.UiEffect.ShowMessage("已清空历史"))
                    load()
                }
                .onFailure { emitEffect(HistoryContract.UiEffect.ShowMessage(it.message ?: "清空失败")) }
        }
    }

    private fun openDetail(videoId: Long) {
        emitEffect(HistoryContract.UiEffect.OpenDetail(videoId))
    }

    private fun commit(mutation: HistoryContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: HistoryContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
