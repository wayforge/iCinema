package com.icinema.domain.model

/**
 * Domain 层视频模型
 * 由 domain 层使用，不依赖 data 层
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
) {
    val playSources: List<PlaySource>
        get() = toPlaySources()

    /**
     * 向后兼容 detail 现有选源/选集 UI。
     */
    val playGroups: List<Pair<String, List<Pair<String, String>>>>
        get() = playSources.map { source ->
            source.key to source.episodes.map { it.title to it.url }
        }

    fun toPlaySources(): List<PlaySource> {
        if (playFrom.isNullOrBlank() || playUrl.isNullOrBlank()) return emptyList()

        val sourceList = playFrom.split("$$$")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val urlGroupList = playUrl.split("$$$")

        return sourceList.mapIndexedNotNull { sourceIndex, sourceKey ->
            val episodes = parseEpisodeGroup(urlGroupList.getOrNull(sourceIndex).orEmpty())
            if (episodes.isEmpty()) null else PlaySource(key = sourceKey, episodes = episodes)
        }
    }

    private fun parseEpisodeGroup(group: String): List<PlayableEpisode> {
        return group.split("#")
            .mapIndexedNotNull { index, rawEpisode ->
                val parts = rawEpisode.split("$", limit = 2)
                if (parts.size != 2) {
                    return@mapIndexedNotNull null
                }

                val title = parts[0].trim().ifBlank { "第${index + 1}集" }
                val url = parts[1].trim()
                if (url.isBlank()) {
                    return@mapIndexedNotNull null
                }

                PlayableEpisode(
                    index = index,
                    title = title,
                    url = url,
                    isHls = url.contains("m3u8", ignoreCase = true)
                )
            }
    }
}
