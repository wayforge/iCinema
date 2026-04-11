package com.icinema.domain.model

data class WatchHistoryItem(
    val id: Long,
    val videoId: Long,
    val videoName: String,
    val videoPic: String,
    val sourceKey: String,
    val episodeIndex: Int,
    val episodeTitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val completed: Boolean
) {
    val progress: Float
        get() = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
