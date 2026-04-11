package com.icinema

import android.app.Application
import com.icinema.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ICinemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}