package com.icinema.data.repository

import com.icinema.data.api.CmsApiService
import com.icinema.data.local.dao.CategoryDao
import com.icinema.data.local.dao.PlaybackHistoryDao
import com.icinema.data.local.dao.FavoriteDao
import com.icinema.data.local.dao.SearchHistoryDao
import com.icinema.data.local.entity.CategoryEntity
import com.icinema.data.mappers.toData
import com.icinema.data.mappers.toDomain
import com.icinema.data.model.Category
import com.icinema.data.model.Video
import com.icinema.domain.model.Category as DomainCategory
import com.icinema.domain.model.Video as DomainVideo
import com.icinema.domain.model.WatchHistoryItem
import com.icinema.domain.model.FavoriteItem
import com.icinema.domain.model.UserSession
import com.icinema.domain.model.DownloadTask
import com.icinema.data.local.entity.FavoriteEntity
import com.icinema.data.local.entity.SearchHistoryEntity
import com.icinema.data.local.dao.DownloadTaskDao
import com.icinema.data.local.entity.DownloadTaskEntity

class CmsRepositoryImpl(
    private val apiService: CmsApiService,
    private val categoryDao: CategoryDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val downloadTaskDao: DownloadTaskDao,
    private val sessionPrefs: android.content.SharedPreferences
) : ICmsRepository {

    override suspend fun getCategoryList(): Result<List<DomainCategory>> {
        return try {

            val cacheCategorys = categoryDao.getVisibleCategories()
            if (cacheCategorys.isNotEmpty()) {
                return Result.success(cacheCategorys.map { it -> it.toData().toDomain() })
            }


            val response = apiService.categoryList()
            if (response.code == 1) {
                val categories: List<Category> = response.classList?.map { it.toData() }
                    ?: response.list?.map { it.toData() }
                    ?: emptyList()

                categoryDao.insertAllCategories(categories.map {
                    CategoryEntity(
                        id = it.id,
                        name = it.name,
                        parentId = it.parentId,
                        currentId = it.id,
                        show = it.show,
                        sort = it.sort
                    )
                })

                Result.success(categories.map { it.toDomain() })
            } else {
                Result.failure(Exception(response.msg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVideoList(
        page: Int,
        categoryId: Int?,
        keyword: String?
    ): Result<List<DomainVideo>> {
        return try {
            val response = apiService.vodDetail(
                page = page,
                categoryId = categoryId,
                keyword = keyword
            )
            if (response.code == 1) {
                val videos = response.list?.map { it.toData().toDomain() } ?: emptyList()
                Result.success(videos)
            } else {
                Result.failure(Exception(response.msg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVideoDetail(videoId: Long): Result<DomainVideo> {
        return try {
            val response = apiService.vodDetail(videoId = videoId)
            if (response.code == 1 && response.list != null && response.list.isNotEmpty()) {
                val video = response.list.first().toData().toDomain()
                Result.success(video)
            } else {
                Result.failure(Exception(response.msg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchVideo(keyword: String, page: Int): Result<List<DomainVideo>> {
        return try {
            val response = apiService.vodDetail(
                keyword = keyword,
                page = page
            )
            if (response.code == 1) {
                val videos = response.list?.map { it.toData().toDomain() } ?: emptyList()
                Result.success(videos)
            } else {
                Result.failure(Exception(response.msg ?: "搜索失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWatchHistory(): Result<List<WatchHistoryItem>> {
        return try {
            val latestByVideo = playbackHistoryDao.getRecentHistory()
            Result.success(latestByVideo.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLatestPlaybackForVideo(videoId: Long): Result<WatchHistoryItem?> {
        return try {
            Result.success(playbackHistoryDao.getLatestHistoryForVideo(videoId)?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContinueWatching(limit: Int): Result<List<WatchHistoryItem>> {
        return try {
            val history = getWatchHistory().getOrThrow()
                .filter { !it.completed && it.durationMs > 0 && it.positionMs > 0 }
                .take(limit)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHistoryItem(id: Long): Result<Unit> {
        return try {
            playbackHistoryDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearWatchHistory(): Result<Unit> {
        return try {
            playbackHistoryDao.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(videoId: Long): Result<Boolean> {
        return try {
            Result.success(favoriteDao.exists(videoId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(video: DomainVideo): Result<Boolean> {
        return try {
            val existed = favoriteDao.exists(video.id)
            if (existed) {
                favoriteDao.deleteByVideoId(video.id)
                Result.success(false)
            } else {
                favoriteDao.insert(
                    FavoriteEntity(
                        videoId = video.id,
                        videoName = video.name,
                        videoPic = video.pic,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(): Result<List<FavoriteItem>> {
        return try {
            Result.success(favoriteDao.getAll().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSearchHistory(limit: Int): Result<List<String>> {
        return try {
            Result.success(searchHistoryDao.getRecent(limit).map { it.keyword })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSearchKeyword(keyword: String): Result<Unit> {
        return try {
            val normalized = keyword.trim()
            if (normalized.isNotEmpty()) {
                searchHistoryDao.insert(
                    SearchHistoryEntity(
                        keyword = normalized,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearSearchHistory(): Result<Unit> {
        return try {
            searchHistoryDao.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHotKeywords(): Result<List<String>> {
        return Result.success(listOf("动作", "科幻", "悬疑", "国产剧", "高分电影"))
    }

    override suspend fun getRecommendedVideos(limit: Int): Result<List<DomainVideo>> {
        return try {
            val watchHistory = playbackHistoryDao.getRecentHistory()
            val continueWatchingIds = watchHistory
                .map { it.toDomain() }
                .filter { !it.completed && it.durationMs > 0 && it.positionMs > 0 }
                .map { it.videoId }
                .toSet()
            val preferredTypeIds = watchHistory.mapNotNull { history ->
                apiService.vodDetail(videoId = history.videoId)
                    .list
                    ?.firstOrNull()
                    ?.typeId
            }

            val recommended = if (preferredTypeIds.isNotEmpty()) {
                val typeId = preferredTypeIds.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                if (typeId != null) {
                    getVideoList(page = 1, categoryId = typeId, keyword = null).getOrThrow()
                } else {
                    getVideoList(page = 1, categoryId = null, keyword = null).getOrThrow()
                }
            } else {
                getVideoList(page = 1, categoryId = null, keyword = null).getOrThrow()
            }

            Result.success(
                recommended
                    .distinctBy { it.id }
                    .filterNot { it.id in continueWatchingIds }
                    .take(limit)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(username: String, password: String): Result<UserSession> {
        return try {
            if (username.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("账号或密码不能为空"))
            }
            val token = "mock_token_${username}_${System.currentTimeMillis()}"
            sessionPrefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_TOKEN, token)
                .putLong(KEY_LOGIN_AT, System.currentTimeMillis())
                .apply()
            Result.success(
                UserSession(
                    username = username,
                    token = token,
                    loginAt = sessionPrefs.getLong(KEY_LOGIN_AT, System.currentTimeMillis())
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            sessionPrefs.edit()
                .remove(KEY_USERNAME)
                .remove(KEY_TOKEN)
                .remove(KEY_LOGIN_AT)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentSession(): Result<UserSession?> {
        return try {
            val username = sessionPrefs.getString(KEY_USERNAME, null)
            val token = sessionPrefs.getString(KEY_TOKEN, null)
            if (username.isNullOrBlank() || token.isNullOrBlank()) {
                Result.success(null)
            } else {
                Result.success(
                    UserSession(
                        username = username,
                        token = token,
                        loginAt = sessionPrefs.getLong(KEY_LOGIN_AT, 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enqueueDownload(videoId: Long, videoName: String, episodeTitle: String): Result<Unit> {
        return try {
            downloadTaskDao.insert(
                DownloadTaskEntity(
                    videoId = videoId,
                    videoName = videoName,
                    episodeTitle = episodeTitle,
                    progress = 0,
                    status = "queued",
                    updatedAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDownloadTasks(): Result<List<DownloadTask>> {
        return try {
            Result.success(downloadTaskDao.getAll().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDownloadTask(id: Long, progress: Int, status: String): Result<Unit> {
        return try {
            downloadTaskDao.update(id, progress.coerceIn(0, 100), status, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.icinema.data.local.entity.PlaybackHistoryEntity.toDomain(): WatchHistoryItem {
        return WatchHistoryItem(
            id = id,
            videoId = videoId,
            videoName = videoName,
            videoPic = videoPic,
            sourceKey = sourceKey,
            episodeIndex = episodeIndex,
            episodeTitle = episodeTitle,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
            completed = completed
        )
    }

    private fun FavoriteEntity.toDomain(): FavoriteItem {
        return FavoriteItem(
            id = id,
            videoId = videoId,
            videoName = videoName,
            videoPic = videoPic,
            updatedAt = updatedAt
        )
    }

    private fun DownloadTaskEntity.toDomain(): DownloadTask {
        return DownloadTask(
            id = id,
            videoId = videoId,
            videoName = videoName,
            episodeTitle = episodeTitle,
            progress = progress,
            status = status,
            updatedAt = updatedAt
        )
    }

    companion object {
        private const val KEY_USERNAME = "session_username"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_LOGIN_AT = "session_login_at"
    }
}
