package com.icinema.pages.player.core

import com.icinema.domain.model.PlayableEpisode
import com.icinema.pages.player.core.hls.HlsSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerPreloadCoordinator @Inject constructor(
    private val hlsSessionManager: HlsSessionManager
) {
    fun preload(videoId: Long, sourceKey: String, episode: PlayableEpisode?) {
        if (episode == null || !episode.isHls) return
        hlsSessionManager.prefetchNextEpisodeManifest(episode.url)
    }

    fun clearFor(videoId: Long, sourceKey: String, episodeIndex: Int) {
        hlsSessionManager.cancelEpisodePrecache()
    }

    fun release() {
        hlsSessionManager.cancelEpisodePrecache()
    }
}
