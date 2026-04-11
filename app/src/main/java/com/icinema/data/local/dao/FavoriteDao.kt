package com.icinema.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.icinema.data.local.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE videoId = :videoId)")
    suspend fun exists(videoId: Long): Boolean

    @Query("SELECT * FROM favorite ORDER BY updatedAt DESC")
    suspend fun getAll(): List<FavoriteEntity>
}
