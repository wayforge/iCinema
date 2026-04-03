package com.icinema.data.api

import com.icinema.data.model.CategoryResponse
import com.icinema.data.model.CmsResponse
import com.icinema.data.model.VideoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 苹果CMS视频API接口
 *
 * 文档参考: https://github.com/magicblack/maccms10/wiki/接口文档
 */
interface CmsApiService {

    /**
     * 获取视频分类列表
     *
     * @param action 固定值: "list"
     * @param typeId 父分类ID，0=获取所有一级分类
     * @return 分类列表数据
     */
    @GET("api.php/provide/vod/")
    suspend fun categoryList(
        @Query("ac") action: String = "list",
        @Query("t") typeId: Int = 0
    ): CmsResponse<List<CategoryResponse>>

    /**
     * 统一的视频详情接口（支持列表/搜索/详情三种模式）
     * 根据传入参数自动判断获取模式：
     *   - 传入videoId时 → 获取单个视频详情
     *   - 传入keyword时 → 搜索视频
     *   - 传入categoryId时 → 按分类获取列表
     *   - 都不传入时 → 获取全部视频列表
     *
     * @param action 固定值: "detail"
     * @param videoId 视频ID，获取单个详情时传入
     * @param categoryId 分类ID，按分类筛选时传入
     * @param keyword 搜索关键词，搜索视频时传入
     * @param page 页码，从1开始，默认1
     * @param limit 每页数量，默认5条
     * @return 视频列表数据
     */
    @GET("api.php/provide/vod/")
    suspend fun vodDetail(
        @Query("ac") action: String = "detail",
        @Query("ids") videoId: Long? = null,
        @Query("t") categoryId: Int? = null,
        @Query("wd") keyword: String? = null,
        @Query("pg") page: Int = 1,
        @Query("limit") limit: String = "5"
    ): CmsResponse<List<VideoResponse>>
}