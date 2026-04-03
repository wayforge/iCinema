package com.icinema

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ICinemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}