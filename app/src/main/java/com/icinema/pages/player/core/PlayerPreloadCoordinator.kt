package com.icinema.pages.player.core

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.icinema.domain.model.PlayableEpisode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerPreloadCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackMediaSourceFactory: PlaybackMediaSourceFactory
) {
    private var preloadPlayer: ExoPlayer? = null
    private var preloadedKey: String? = null

    fun preload(videoId: Long, sourceKey: String, episode: PlayableEpisode?) {
        if (episode == null || !episode.isHls) {
            release()
            return
        }

        val cacheKey = buildKey(videoId, sourceKey, episode.index)
        if (cacheKey == preloadedKey) {
            return
        }

        release()
        preloadedKey = cacheKey
        preloadPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(playbackMediaSourceFactory.createMediaSourceFactory())
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(episode.url))
                prepare()
                playWhenReady = false
                volume = 0f
            }
    }

    fun clearFor(videoId: Long, sourceKey: String, episodeIndex: Int) {
        val key = buildKey(videoId, sourceKey, episodeIndex)
        if (preloadedKey == key) {
            release()
        }
    }

    fun release() {
        preloadPlayer?.release()
        preloadPlayer = null
        preloadedKey = null
    }

    private fun buildKey(videoId: Long, sourceKey: String, episodeIndex: Int): String {
        return "$videoId:$sourceKey:$episodeIndex"
    }
}