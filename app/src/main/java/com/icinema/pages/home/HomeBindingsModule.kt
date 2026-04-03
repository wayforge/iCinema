package com.icinema.pages.home

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class HomeBindingsModule {
    @Binds
    abstract fun bindHomeBizPort(
        impl: RepositoryHomeBizPort
    ): HomeBizPort
}
