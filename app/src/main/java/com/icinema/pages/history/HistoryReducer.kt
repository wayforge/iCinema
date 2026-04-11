package com.icinema.pages.history

import javax.inject.Inject

class HistoryReducer @Inject constructor() {
    fun reduce(current: HistoryContract.UiState, mutation: HistoryContract.Mutation): HistoryContract.UiState {
        return when (mutation) {
            HistoryContract.Mutation.LoadStarted -> current.copy(isLoading = true, error = null)
            is HistoryContract.Mutation.LoadSucceeded -> current.copy(
                isLoading = false,
                items = mutation.items,
                error = null
            )
            is HistoryContract.Mutation.LoadFailed -> current.copy(
                isLoading = false,
                error = mutation.message
            )
        }
    }
}
