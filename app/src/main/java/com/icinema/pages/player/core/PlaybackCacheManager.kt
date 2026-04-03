package com.icinema.pages.player.core

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }
    private val cacheDir by lazy { File(context.cacheDir, "media3") }

    val cache: SimpleCache by lazy {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            databaseProvider
        )
    }

    companion object {
        private const val MAX_CACHE_BYTES = 1_500L * 1024L * 1024L
    }
}