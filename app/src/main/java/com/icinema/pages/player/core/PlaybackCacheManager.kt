package com.icinema.pages.player.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val hlsCacheDir: File by lazy {
        File(context.noBackupFilesDir, "hls-proxy")
    }

    val maxCacheBytes: Long = MAX_CACHE_BYTES

    companion object {
        private const val MAX_CACHE_BYTES = 3_000L * 1024L * 1024L
    }
}
