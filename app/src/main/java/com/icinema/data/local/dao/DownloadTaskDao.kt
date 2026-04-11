package com.icinema.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.icinema.data.local.entity.DownloadTaskEntity

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadTaskEntity)

    @Query("SELECT * FROM download_task ORDER BY updatedAt DESC")
    suspend fun getAll(): List<DownloadTaskEntity>

    @Query("UPDATE download_task SET progress = :progress, status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, progress: Int, status: String, updatedAt: Long)

    @Query("DELETE FROM download_task WHERE id = :id")
    suspend fun delete(id: Long)
}
