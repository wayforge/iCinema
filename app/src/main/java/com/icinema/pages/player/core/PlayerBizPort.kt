package com.icinema.pages.player

import android.content.SharedPreferences
import javax.inject.Named
import com.icinema.data.local.dao.PlaybackHistoryDao
import com.icinema.data.local.entity.PlaybackHistoryEntity
import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.Video
import javax.inject.Inject

data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long
)

data class PlayerSettings(
    val playbackSpeed: Float = 1.0f,
    val autoPlayNextEnabled: Boolean = true,
    val gestureSeekEnabled: Boolean = true
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
        videoName: String,
        videoPic: String,
        sourceKey: String,
        episodeIndex: Int,
        episodeTitle: String,
        positionMs: Long,
        durationMs: Long
    )

    suspend fun markProgressCompleted(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    )

    suspend fun loadPlayerSettings(): PlayerSettings

    suspend fun savePlayerSettings(settings: PlayerSettings)
}

class RepositoryPlayerBizPort @Inject constructor(
    private val repository: ICmsRepository,
    private val playbackHistoryDao: PlaybackHistoryDao,
    @Named("player_prefs") private val playerPrefs: SharedPreferences
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
        videoName: String,
        videoPic: String,
        sourceKey: String,
        episodeIndex: Int,
        episodeTitle: String,
        positionMs: Long,
        durationMs: Long
    ) {
        playbackHistoryDao.insert(
            PlaybackHistoryEntity(
                videoId = videoId,
                videoName = videoName,
                videoPic = videoPic,
                sourceKey = sourceKey,
                episodeIndex = episodeIndex,
                episodeTitle = episodeTitle,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
                completed = false
            )
        )
    }

    override suspend fun markProgressCompleted(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ) {
        playbackHistoryDao.markCompleted(videoId, sourceKey, episodeIndex, System.currentTimeMillis())
    }

    override suspend fun loadPlayerSettings(): PlayerSettings {
        return PlayerSettings(
            playbackSpeed = playerPrefs.getFloat(KEY_SPEED, 1.0f),
            autoPlayNextEnabled = playerPrefs.getBoolean(KEY_AUTOPLAY_NEXT, true),
            gestureSeekEnabled = playerPrefs.getBoolean(KEY_GESTURE_SEEK, true)
        )
    }

    override suspend fun savePlayerSettings(settings: PlayerSettings) {
        playerPrefs.edit()
            .putFloat(KEY_SPEED, settings.playbackSpeed)
            .putBoolean(KEY_AUTOPLAY_NEXT, settings.autoPlayNextEnabled)
            .putBoolean(KEY_GESTURE_SEEK, settings.gestureSeekEnabled)
            .apply()
    }

    companion object {
        private const val KEY_SPEED = "player_speed"
        private const val KEY_AUTOPLAY_NEXT = "player_auto_next"
        private const val KEY_GESTURE_SEEK = "player_gesture_seek"
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
        videoName: String,
        videoPic: String,
        sourceKey: String,
        episodeIndex: Int,
        episodeTitle: String,
        positionMs: Long,
        durationMs: Long
    ) = Unit

    override suspend fun markProgressCompleted(
        videoId: Long,
        sourceKey: String,
        episodeIndex: Int
    ) = Unit

    override suspend fun loadPlayerSettings(): PlayerSettings {
        return PlayerSettings()
    }

    override suspend fun savePlayerSettings(settings: PlayerSettings) = Unit
}
