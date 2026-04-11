package com.icinema.domain.model

data class DownloadTask(
    val id: Long,
    val videoId: Long,
    val videoName: String,
    val episodeTitle: String,
    val progress: Int,
    val status: String,
    val updatedAt: Long
)
