package com.icinema.data.model

import com.google.gson.annotations.SerializedName

data class CmsResponse<T>(
    @SerializedName("code")
    val code: Int,
    @SerializedName("msg")
    val msg: String?,
    @SerializedName("page")
    val page: Int?,
    @SerializedName("pagecount")
    val pageCount: Int?,
    @SerializedName("limit")
    val limit: Int?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("list")
    val list: T?,
    @SerializedName("class")
    val classList: List<CategoryResponse>?
)