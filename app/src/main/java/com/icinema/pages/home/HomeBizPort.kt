package com.icinema.pages.home

import com.icinema.R
import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.Category
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import javax.inject.Inject
import kotlinx.coroutines.delay

interface HomeBizPort {
    suspend fun loadCategories(): Result<List<Category>>
    suspend fun loadVideos(page: Int, categoryId: Int?, keyword: String?): Result<List<Video>>
    suspend fun searchVideos(keyword: String, page: Int): Result<List<Video>>
    suspend fun loadContinueWatching(limit: Int = 10): Result<List<WatchHistoryItem>>
    suspend fun loadSearchHistory(limit: Int = 20): Result<List<String>>
    suspend fun loadHotKeywords(): Result<List<String>>
    suspend fun saveSearchKeyword(keyword: String): Result<Unit>
    suspend fun clearSearchHistory(): Result<Unit>
    suspend fun loadRecommendations(limit: Int = 10): Result<List<Video>>
}

class RepositoryHomeBizPort @Inject constructor(
    private val repository: ICmsRepository
) : HomeBizPort {
    override suspend fun loadCategories(): Result<List<Category>> {
        return repository.getCategoryList()
    }

    override suspend fun loadVideos(page: Int, categoryId: Int?, keyword: String?): Result<List<Video>> {
        return repository.getVideoList(
            page = page,
            categoryId = categoryId,
            keyword = keyword
        )
    }

    override suspend fun searchVideos(keyword: String, page: Int): Result<List<Video>> {
        return repository.searchVideo(keyword = keyword, page = page)
    }

    override suspend fun loadContinueWatching(limit: Int): Result<List<WatchHistoryItem>> {
        return repository.getContinueWatching(limit)
    }

    override suspend fun loadSearchHistory(limit: Int): Result<List<String>> {
        return repository.getSearchHistory(limit)
    }

    override suspend fun loadHotKeywords(): Result<List<String>> {
        return repository.getHotKeywords()
    }

    override suspend fun saveSearchKeyword(keyword: String): Result<Unit> {
        return repository.saveSearchKeyword(keyword)
    }

    override suspend fun clearSearchHistory(): Result<Unit> {
        return repository.clearSearchHistory()
    }

    override suspend fun loadRecommendations(limit: Int): Result<List<Video>> {
        return repository.getRecommendedVideos(limit)
    }
}

class FakeHomeBizPort(
    private val delayMs: Long = 500L
) : HomeBizPort {
    override suspend fun loadCategories(): Result<List<Category>> {
        delay(delayMs)
        return Result.success(sampleCategories)
    }

    override suspend fun loadVideos(page: Int, categoryId: Int?, keyword: String?): Result<List<Video>> {
        delay(delayMs)
        return sampleResult(page = page, categoryId = categoryId, keyword = keyword)
    }

    override suspend fun searchVideos(keyword: String, page: Int): Result<List<Video>> {
        delay(delayMs)
        return sampleResult(page = page, categoryId = null, keyword = keyword)
    }

    override suspend fun loadContinueWatching(limit: Int): Result<List<WatchHistoryItem>> {
        delay(delayMs)
        return Result.success(emptyList())
    }

    override suspend fun loadSearchHistory(limit: Int): Result<List<String>> {
        delay(delayMs)
        return Result.success(listOf("复仇者", "三体", "盗梦空间"))
    }

    override suspend fun loadHotKeywords(): Result<List<String>> {
        delay(delayMs)
        return Result.success(listOf("动作", "悬疑", "科幻"))
    }

    override suspend fun saveSearchKeyword(keyword: String): Result<Unit> {
        delay(delayMs)
        return Result.success(Unit)
    }

    override suspend fun clearSearchHistory(): Result<Unit> {
        delay(delayMs)
        return Result.success(Unit)
    }

    override suspend fun loadRecommendations(limit: Int): Result<List<Video>> {
        delay(delayMs)
        return sampleResult(page = 1, categoryId = null, keyword = null)
            .map { it.take(limit) }
    }

    private fun sampleResult(page: Int, categoryId: Int?, keyword: String?): Result<List<Video>> {
        if (page <= 0) {
            return Result.failure(IllegalArgumentException("Invalid page: $page"))
        }
        if (keyword?.contains("error", ignoreCase = true) == true) {
            return Result.failure(IllegalStateException("Fake search error for keyword=$keyword"))
        }

        val filtered = sampleVideos
            .filter { video ->
                categoryId == null || video.typeId == categoryId
            }
            .filter { video ->
                keyword.isNullOrBlank() ||
                    video.name.contains(keyword, ignoreCase = true) ||
                    (video.actor?.contains(keyword, ignoreCase = true) == true) ||
                    (video.director?.contains(keyword, ignoreCase = true) == true)
            }

        val pageSize = 6
        val fromIndex = (page - 1) * pageSize
        if (fromIndex >= filtered.size) {
            return Result.success(emptyList())
        }
        val toIndex = minOf(fromIndex + pageSize, filtered.size)
        return Result.success(filtered.subList(fromIndex, toIndex))
    }

    private val sampleCategories = listOf(
        Category(id = 1, name = "电影", parentId = null),
        Category(id = 2, name = "剧集", parentId = null),
        Category(id = 3, name = "纪录片", parentId = null)
    )

    private val localPoster = "android.resource://com.icinema/${R.drawable.tktk_house_cabinet_seat}"

    private val sampleVideos = listOf(
        sampleVideo(1, "城市追光", 1, "电影", "林越", "顾川"),
        sampleVideo(2, "午夜航线", 1, "电影", "许棠", "秦策"),
        sampleVideo(3, "雾港档案", 2, "剧集", "周然", "沈季"),
        sampleVideo(4, "回声剧场", 2, "剧集", "夏弥", "陆衡"),
        sampleVideo(5, "北纬长河", 3, "纪录片", "旁白", "纪实组"),
        sampleVideo(6, "候鸟日记", 3, "纪录片", "旁白", "纪实组"),
        sampleVideo(7, "玻璃深海", 1, "电影", "江屿", "宋祈"),
        sampleVideo(8, "夏末来信", 1, "电影", "顾遥", "韩彻"),
        sampleVideo(9, "边境信号", 2, "剧集", "岳临", "钟墨"),
        sampleVideo(10, "白昼剧场", 2, "剧集", "乔木", "庄唯"),
        sampleVideo(11, "极地以南", 3, "纪录片", "旁白", "观察室"),
        sampleVideo(12, "深空手册", 3, "纪录片", "旁白", "观察室")
    )

    private fun sampleVideo(
        id: Long,
        name: String,
        typeId: Int,
        typeName: String,
        actor: String,
        director: String
    ): Video {
        return Video(
            id = id,
            name = name,
            pic = localPoster,
            picThumb = localPoster,
            actor = actor,
            director = director,
            content = "$name 的占位剧情简介",
            area = "中国",
            year = "2026",
            typeId = typeId,
            typeName = typeName,
            playFrom = null,
            playUrl = null,
            total = null
        )
    }
}
