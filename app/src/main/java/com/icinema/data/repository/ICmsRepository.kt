package com.icinema.data.repository

import com.icinema.domain.model.Category
import com.icinema.domain.model.Video

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
}