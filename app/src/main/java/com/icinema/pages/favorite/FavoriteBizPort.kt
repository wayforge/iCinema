package com.icinema.pages.favorite

import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.FavoriteItem
import javax.inject.Inject

interface FavoriteBizPort {
    suspend fun loadFavorites(): Result<List<FavoriteItem>>
}

class RepositoryFavoriteBizPort @Inject constructor(
    private val repository: ICmsRepository
) : FavoriteBizPort {
    override suspend fun loadFavorites(): Result<List<FavoriteItem>> {
        return repository.getFavorites()
    }
}
