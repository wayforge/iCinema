package com.icinema.pages.detail

import com.icinema.R
import com.icinema.data.repository.ICmsRepository
import com.icinema.domain.model.Video
import com.icinema.domain.model.WatchHistoryItem
import javax.inject.Inject
import kotlinx.coroutines.delay

interface DetailBizPort {
    suspend fun loadVideo(videoId: Long): Result<Video>
    suspend fun loadLatestPlayback(videoId: Long): Result<WatchHistoryItem?>
    suspend fun isFavorite(videoId: Long): Result<Boolean>
    suspend fun toggleFavorite(video: Video): Result<Boolean>
}

class RepositoryDetailBizPort @Inject constructor(
    private val repository: ICmsRepository
) : DetailBizPort {
    override suspend fun loadVideo(videoId: Long): Result<Video> {
        return repository.getVideoDetail(videoId)
    }

    override suspend fun loadLatestPlayback(videoId: Long): Result<WatchHistoryItem?> {
        return repository.getLatestPlaybackForVideo(videoId)
    }

    override suspend fun isFavorite(videoId: Long): Result<Boolean> {
        return repository.isFavorite(videoId)
    }

    override suspend fun toggleFavorite(video: Video): Result<Boolean> {
        return repository.toggleFavorite(video)
    }
}

class FakeDetailBizPort(
    private val delayMs: Long = 500L
) : DetailBizPort {
    private val localPoster = "android.resource://com.icinema/${R.drawable.tktk_house_cabinet_seat}"

    override suspend fun loadVideo(videoId: Long): Result<Video> {
        delay(delayMs)
        if (videoId <= 0L) {
            return Result.failure(IllegalArgumentException("Invalid videoId=$videoId"))
        }
        return Result.success(
            Video(
                id = videoId,
                name = "云海长歌",
                pic = localPoster,
                picThumb = localPoster,
                actor = "沈砚, 顾晚舟, 许临川",
                director = "林叙",
                content = "<p>一部用于 MVI 与数据闭环演练的详情页样板数据，包含双播放源、分段选集与简介信息。</p>",
                area = "中国",
                year = "2026",
                typeId = 13,
                typeName = "国产剧",
                playFrom = "stable\$\$\$backup",
                playUrl = buildString {
                    append(
                        listOf(
                            "第01集\$https://example.com/stable/01.m3u8",
                            "第02集\$https://example.com/stable/02.m3u8",
                            "第03集\$https://example.com/stable/03.m3u8",
                            "第04集\$https://example.com/stable/04.m3u8",
                            "第05集\$https://example.com/stable/05.m3u8",
                            "第06集\$https://example.com/stable/06.m3u8",
                            "第07集\$https://example.com/stable/07.m3u8",
                            "第08集\$https://example.com/stable/08.m3u8",
                            "第09集\$https://example.com/stable/09.m3u8",
                            "第10集\$https://example.com/stable/10.m3u8",
                            "第11集\$https://example.com/stable/11.m3u8",
                            "第12集\$https://example.com/stable/12.m3u8"
                        ).joinToString("#")
                    )
                    append("\$\$\$")
                    append(
                        listOf(
                            "第01集\$https://example.com/backup/01.m3u8",
                            "第02集\$https://example.com/backup/02.m3u8",
                            "第03集\$https://example.com/backup/03.m3u8",
                            "第04集\$https://example.com/backup/04.m3u8"
                        ).joinToString("#")
                    )
                },
                total = 12
            )
        )
    }

    override suspend fun loadLatestPlayback(videoId: Long): Result<WatchHistoryItem?> = Result.success(null)

    override suspend fun isFavorite(videoId: Long): Result<Boolean> = Result.success(false)

    override suspend fun toggleFavorite(video: Video): Result<Boolean> = Result.success(true)
}
