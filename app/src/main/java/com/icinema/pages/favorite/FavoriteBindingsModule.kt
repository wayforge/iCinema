package com.icinema.pages.favorite

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class FavoriteBindingsModule {
    @Binds
    abstract fun bindFavoriteBizPort(
        impl: RepositoryFavoriteBizPort
    ): FavoriteBizPort
}
