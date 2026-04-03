package com.icinema.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val parentId: Int?,
    val currentId: Int,
    val show: Boolean = true,
    val sort: Int = 0
)