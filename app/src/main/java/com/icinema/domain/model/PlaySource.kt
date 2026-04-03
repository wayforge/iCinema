package com.icinema.domain.model

data class PlaySource(
    val key: String,
    val episodes: List<PlayableEpisode>
)
