package com.icinema.pages.favorite

import javax.inject.Inject

class FavoriteReducer @Inject constructor() {
    fun reduce(current: FavoriteContract.UiState, mutation: FavoriteContract.Mutation): FavoriteContract.UiState {
        return when (mutation) {
            FavoriteContract.Mutation.LoadStarted -> current.copy(isLoading = true, error = null)
            is FavoriteContract.Mutation.LoadSucceeded -> current.copy(
                isLoading = false,
                items = mutation.items,
                error = null
            )
            is FavoriteContract.Mutation.LoadFailed -> current.copy(
                isLoading = false,
                error = mutation.message
            )
        }
    }
}
