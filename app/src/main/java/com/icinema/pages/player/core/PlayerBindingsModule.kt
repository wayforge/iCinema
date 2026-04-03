package com.icinema.pages.player

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class PlayerBindingsModule {
    @Binds
    abstract fun bindPlayerBizPort(
        impl: RepositoryPlayerBizPort
    ): PlayerBizPort
}
