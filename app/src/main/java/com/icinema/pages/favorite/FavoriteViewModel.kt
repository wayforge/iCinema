package com.icinema.pages.favorite

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
class FavoriteViewModel @Inject constructor(
    private val bizPort: FavoriteBizPort,
    private val reducer: FavoriteReducer
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteContract.UiState())
    val uiState: StateFlow<FavoriteContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<FavoriteContract.UiEffect>()
    val uiEffect: Flow<FavoriteContract.UiEffect> = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: FavoriteContract.UiIntent) {
        when (intent) {
            FavoriteContract.UiIntent.Load -> load()
            is FavoriteContract.UiIntent.OpenDetail -> openDetail(intent.videoId)
        }
    }

    private fun load() {
        viewModelScope.launch {
            commit(FavoriteContract.Mutation.LoadStarted)
            bizPort.loadFavorites()
                .onSuccess { commit(FavoriteContract.Mutation.LoadSucceeded(it)) }
                .onFailure { commit(FavoriteContract.Mutation.LoadFailed(it.message ?: "收藏加载失败")) }
        }
    }

    private fun openDetail(videoId: Long) {
        emitEffect(FavoriteContract.UiEffect.OpenDetail(videoId))
    }

    private fun commit(mutation: FavoriteContract.Mutation) {
        _uiState.value = reducer.reduce(_uiState.value, mutation)
    }

    private fun emitEffect(effect: FavoriteContract.UiEffect) {
        _uiEffect.trySend(effect)
    }
}
