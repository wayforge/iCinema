package com.icinema.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_task")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: Long,
    val videoName: String,
    val episodeTitle: String,
    val progress: Int,
    val status: String,
    val updatedAt: Long
)
