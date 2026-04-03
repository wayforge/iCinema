package com.icinema.data.model

import com.google.gson.annotations.SerializedName

data class VideoResponse(
    @SerializedName("vod_id")
    val id: Long,
    @SerializedName("vod_name")
    val name: String,
    @SerializedName("vod_pic")
    val pic: String,
    @SerializedName("vod_pic_thumb")
    val picThumb: String?,
    @SerializedName("vod_actor")
    val actor: String?,
    @SerializedName("vod_director")
    val director: String?,
    @SerializedName("vod_content")
    val content: String?,
    @SerializedName("vod_area")
    val area: String?,
    @SerializedName("vod_year")
    val year: String?,
    @SerializedName("type_id")
    val typeId: Int?,
    @SerializedName("type_name")
    val typeName: String?,
    @SerializedName("vod_play_from")
    val playFrom: String?,
    @SerializedName("vod_play_url")
    val playUrl: String?,
    @SerializedName("vod_total")
    val total: Int?
)

data class CategoryResponse(
    @SerializedName("type_id")
    val id: Int,
    @SerializedName("type_name")
    val name: String,
    @SerializedName("type_pid")
    val parentId: Int?
)