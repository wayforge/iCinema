package com.icinema.data.repository

import com.icinema.data.api.CmsApiService
import com.icinema.data.local.dao.CategoryDao
import com.icinema.data.local.entity.CategoryEntity
import com.icinema.data.mappers.toData
import com.icinema.data.mappers.toDomain
import com.icinema.data.model.Category
import com.icinema.data.model.Video
import com.icinema.domain.model.Category as DomainCategory
import com.icinema.domain.model.Video as DomainVideo

class CmsRepositoryImpl(
    private val apiService: CmsApiService,
    private val categoryDao: CategoryDao
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

}