package com.icinema.domain.model

data class PlayableEpisode(
    val index: Int,
    val title: String,
    val url: String,
    val isHls: Boolean
)
