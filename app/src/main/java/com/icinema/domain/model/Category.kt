package com.icinema.domain.model

/**
 * Domain 层分类模型
 * 由 domain 层使用，不依赖 data 层
 */
data class Category(
    val id: Int,
    val name: String,
    val parentId: Int?,
    val currentId: Int = id,
    val show: Boolean = true,
    val sort: Int = 0
)
