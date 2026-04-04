package com.icinema.pages.category

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class CategoryBindingsModule {
    @Binds
    abstract fun bindCategoryBizPort(
        impl: RepositoryCategoryBizPort
    ): CategoryBizPort
}
