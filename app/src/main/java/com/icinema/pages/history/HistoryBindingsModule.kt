package com.icinema.pages.history

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class HistoryBindingsModule {
    @Binds
    abstract fun bindHistoryBizPort(
        impl: RepositoryHistoryBizPort
    ): HistoryBizPort
}
