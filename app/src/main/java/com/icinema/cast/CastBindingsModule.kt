package com.icinema.cast

import com.icinema.cast.dlna.DlnaCastController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class CastBindingsModule {
    @Binds
    abstract fun bindCastController(
        impl: DlnaCastController
    ): CastController
}
