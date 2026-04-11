package com.icinema.data.repository

import com.icinema.domain.model.Category
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import com.icinema.domain.model.FavoriteItem
import com.icinema.domain.model.UserSession
import com.icinema.domain.model.DownloadTask

interface ICmsRepository {
    suspend fun getCategoryList(): Result<List<Category>>

    suspend fun getVideoList(
        page: Int = 1,
        categoryId: Int? = null,
        keyword: String? = null
    ): Result<List<Video>>

    suspend fun getVideoDetail(videoId: Long): Result<Video>

    suspend fun searchVideo(
        keyword: String,
        page: Int = 1
    ): Result<List<Video>>

    suspend fun getWatchHistory(): Result<List<WatchHistoryItem>>

    suspend fun getLatestPlaybackForVideo(videoId: Long): Result<WatchHistoryItem?>

    suspend fun getContinueWatching(limit: Int = 10): Result<List<WatchHistoryItem>>

    suspend fun deleteHistoryItem(id: Long): Result<Unit>

    suspend fun clearWatchHistory(): Result<Unit>

    suspend fun isFavorite(videoId: Long): Result<Boolean>

    suspend fun toggleFavorite(video: Video): Result<Boolean>

    suspend fun getFavorites(): Result<List<FavoriteItem>>

    suspend fun getSearchHistory(limit: Int = 20): Result<List<String>>

    suspend fun saveSearchKeyword(keyword: String): Result<Unit>

    suspend fun clearSearchHistory(): Result<Unit>

    suspend fun getHotKeywords(): Result<List<String>>

    suspend fun getRecommendedVideos(limit: Int = 10): Result<List<Video>>

    suspend fun login(username: String, password: String): Result<UserSession>

    suspend fun logout(): Result<Unit>

    suspend fun getCurrentSession(): Result<UserSession?>

    suspend fun enqueueDownload(videoId: Long, videoName: String, episodeTitle: String): Result<Unit>

    suspend fun getDownloadTasks(): Result<List<DownloadTask>>

    suspend fun updateDownloadTask(id: Long, progress: Int, status: String): Result<Unit>
}