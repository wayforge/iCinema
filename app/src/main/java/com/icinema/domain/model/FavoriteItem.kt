package com.icinema.domain.model

data class FavoriteItem(
    val id: Long,
    val videoId: Long,
    val videoName: String,
    val videoPic: String,
    val updatedAt: Long
)
