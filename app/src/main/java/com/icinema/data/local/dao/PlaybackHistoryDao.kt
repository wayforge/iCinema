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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistoryEntity)

    @Query(
        """
        DELETE FROM playback_history
        WHERE videoId = :videoId AND sourceKey = :sourceKey AND episodeIndex = :episodeIndex
        """
    )
    suspend fun delete(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    )
}
