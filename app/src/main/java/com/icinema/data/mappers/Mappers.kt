package com.icinema.data.mappers

import com.icinema.data.local.entity.CategoryEntity
import com.icinema.data.model.Category
import com.icinema.data.model.CategoryResponse
import com.icinema.data.model.Video
import com.icinema.data.model.VideoResponse
import com.icinema.domain.model.Category as DomainCategory
import com.icinema.domain.model.Video as DomainVideo

// ==================== API Response 转 Data 层模型 ====================

fun CategoryResponse.toData(): Category {
    return Category(
        id = id,
        name = name,
        parentId = parentId
    )
}

fun VideoResponse.toData(): Video {
    return Video(
        id = id,
        name = name,
        pic = pic ?: "",
        picThumb = picThumb,
        actor = actor,
        director = director,
        content = content,
        area = area,
        year = year,
        typeId = typeId,
        typeName = typeName,
        playFrom = playFrom,
        playUrl = playUrl,
        total = total
    )
}

// ==================== Entity 转 Data 层模型 ====================

fun CategoryEntity.toData(): Category {
    return Category(
        id = id,
        name = name,
        parentId = parentId,
        currentId = currentId,
        show = show,
        sort = sort
    )
}

// ==================== Data 层模型转 Domain 层模型 ====================

fun Category.toDomain(): DomainCategory {
    return DomainCategory(
        id = id,
        name = name,
        parentId = parentId,
        show = show,
        sort = sort
    )
}

fun Video.toDomain(): DomainVideo {
    return DomainVideo(
        id = id,
        name = name,
        pic = pic,
        picThumb = picThumb,
        actor = actor,
        director = director,
        content = content,
        area = area,
        year = year,
        typeId = typeId,
        typeName = typeName,
        playFrom = playFrom,
        playUrl = playUrl,
        total = total
    )
}

// ==================== 数据格式解析工具 ====================

fun parsePlayUrl(playUrl: String?): List<Pair<String, String>> {
    if (playUrl.isNullOrEmpty()) return emptyList()

    val result = mutableListOf<Pair<String, String>>()

    val playGroups = playUrl.split("$$$")
    if (playGroups.size >= 2) {
        val sources = playGroups[0].split("#")
        val urls = playGroups[1].split("#")

        if (sources.size == urls.size) {
            sources.forEachIndexed { index, source ->
                result.add(source to urls.getOrElse(index) { "" })
            }
        }
    } else {
        val episodes = playUrl.split("#")
        episodes.forEach { episode ->
            val parts = episode.split("$")
            if (parts.size == 2) {
                result.add(parts[0] to parts[1])
            }
        }
    }

    return result
}
