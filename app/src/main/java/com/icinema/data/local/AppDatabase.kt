package com.icinema.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.icinema.data.local.dao.PlaybackHistoryDao
import com.icinema.data.local.dao.CategoryDao
import com.icinema.data.local.entity.CategoryEntity
import com.icinema.data.local.entity.PlaybackHistoryEntity
import com.icinema.util.Converters

@Database(
    entities = [CategoryEntity::class, PlaybackHistoryEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
}
