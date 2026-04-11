package com.icinema.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.icinema.data.local.entity.PlaybackHistoryEntity

@Dao
interface PlaybackHistoryDao {
    @Query(
        """
        SELECT * FROM playback_history
        WHERE videoId = :videoId AND sourceKey = :sourceKey AND episodeIndex = :episodeIndex
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getPlaybackHistory(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ): PlaybackHistoryEntity?

    @Query(
        """
        SELECT * FROM playback_history
        WHERE videoId = :videoId
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestHistoryForVideo(videoId: Long): PlaybackHistoryEntity?

    @Query(
        """
        SELECT ph.* FROM playback_history ph
        INNER JOIN (
            SELECT videoId, MAX(updatedAt) AS maxUpdatedAt
            FROM playback_history
            GROUP BY videoId
        ) latest
        ON ph.videoId = latest.videoId AND ph.updatedAt = latest.maxUpdatedAt
        ORDER BY ph.updatedAt DESC
        """
    )
    suspend fun getRecentHistory(): List<PlaybackHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistoryEntity)

    @Query(
        """
        UPDATE playback_history
        SET completed = 1,
            positionMs = durationMs,
            updatedAt = :updatedAt
        WHERE videoId = :videoId AND sourceKey = :sourceKey AND episodeIndex = :episodeIndex
        """
    )
    suspend fun markCompleted(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int,
        updatedAt: Long
    )

    @Query(
        """
        DELETE FROM playback_history
        WHERE id = :id
        """
    )
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()
}
