package com.icinema.pages.player

import com.icinema.data.local.dao.PlaybackHistoryDao
import com.icinema.data.local.entity.PlaybackHistoryEntity
import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.Video
import javax.inject.Inject

data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long
)

interface PlayerBizPort {
    suspend fun loadVideo(videoId: Long): Result<Video>

    suspend fun loadSavedProgress(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ): PlaybackProgress?

    suspend fun saveProgress(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int,
        positionMs: Long,
        durationMs: Long
    )

    suspend fun clearProgressOnComplete(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    )
}

class RepositoryPlayerBizPort @Inject constructor(
    private val repository: ICmsRepository,
    private val playbackHistoryDao: PlaybackHistoryDao
) : PlayerBizPort {
    override suspend fun loadVideo(videoId: Long): Result<Video> {
        return repository.getVideoDetail(videoId)
    }

    override suspend fun loadSavedProgress(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ): PlaybackProgress? {
        return playbackHistoryDao.getPlaybackHistory(videoId, sourceKey, episodeIndex)
            ?.let { PlaybackProgress(positionMs = it.positionMs, durationMs = it.durationMs) }
    }

    override suspend fun saveProgress(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int,
        positionMs: Long,
        durationMs: Long
    ) {
        playbackHistoryDao.insert(
            PlaybackHistoryEntity(
                videoId = videoId,
                sourceKey = sourceKey,
                episodeIndex = episodeIndex,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearProgressOnComplete(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ) {
        playbackHistoryDao.delete(videoId, sourceKey, episodeIndex)
    }
}

class FakePlayerBizPort @Inject constructor() : PlayerBizPort {
    override suspend fun loadVideo(videoId: Long): Result<Video> {
        return Result.failure(IllegalStateException("FakePlayerBizPort is not bound for runtime playback"))
    }

    override suspend fun loadSavedProgress(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ): PlaybackProgress? {
        return null
    }

    override suspend fun saveProgress(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int,
        positionMs: Long,
        durationMs: Long
    ) = Unit

    override suspend fun clearProgressOnComplete(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ) = Unit
}
