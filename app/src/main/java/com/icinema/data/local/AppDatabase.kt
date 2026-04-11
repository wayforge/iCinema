package com.icinema.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.icinema.data.local.dao.PlaybackHistoryDao
import com.icinema.data.local.dao.CategoryDao
import com.icinema.data.local.dao.FavoriteDao
import com.icinema.data.local.dao.SearchHistoryDao
import com.icinema.data.local.dao.DownloadTaskDao
import com.icinema.data.local.entity.CategoryEntity
import com.icinema.data.local.entity.PlaybackHistoryEntity
import com.icinema.data.local.entity.FavoriteEntity
import com.icinema.data.local.entity.SearchHistoryEntity
import com.icinema.data.local.entity.DownloadTaskEntity
import com.icinema.util.Converters

@Database(
    entities = [CategoryEntity::class, PlaybackHistoryEntity::class, FavoriteEntity::class, SearchHistoryEntity::class, DownloadTaskEntity::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadTaskDao(): DownloadTaskDao
}
