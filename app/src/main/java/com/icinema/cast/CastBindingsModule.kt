package com.icinema.cast

import com.icinema.cast.dlna.DlnaCastController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CastBindingsModule {
    @Binds
    @Singleton
    abstract fun bindCastController(
        impl: DlnaCastController
    ): CastController
}
