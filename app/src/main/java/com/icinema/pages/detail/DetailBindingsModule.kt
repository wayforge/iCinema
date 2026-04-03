package com.icinema.pages.detail

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class DetailBindingsModule {
    @Binds
    abstract fun bindDetailBizPort(
        impl: RepositoryDetailBizPort
    ): DetailBizPort
}
