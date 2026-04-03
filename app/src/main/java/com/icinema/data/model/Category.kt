package com.icinema.data.model

/**
 * Data 层分类数据模型
 * 由 data 层维护，对外暴露给 domain 层使用
 */
data class Category(
    val id: Int,
    val name: String,
    val parentId: Int?,
    val currentId: Int = id,
    val show: Boolean = true,
    val sort: Int = 0
)
