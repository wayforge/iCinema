package com.icinema.data.model

/**
 * Data 层视频数据模型
 * 由 data 层维护，对外暴露给 domain 层使用
 */
data class Video(
    val id: Long,
    val name: String,
    val pic: String,
    val picThumb: String?,
    val actor: String?,
    val director: String?,
    val content: String?,
    val area: String?,
    val year: String?,
    val typeId: Int?,
    val typeName: String?,
    val playFrom: String?,
    val playUrl: String?,
    val total: Int?
)
