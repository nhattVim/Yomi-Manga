package com.example.yomi_manga.data.model

import com.google.gson.annotations.SerializedName

data class Manga(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("cover")
    val cover: String? = null,
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("genres")
    val genres: List<String>? = null,
    @SerializedName("chapters")
    val chapters: List<Chapter>? = null,
    @SerializedName("rating")
    val rating: Double? = null,
    @SerializedName("views")
    val views: Long? = null
)

data class Chapter(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("number")
    val number: Int? = null,
    @SerializedName("images")
    val images: List<String>? = null,
    @SerializedName("uploadDate")
    val uploadDate: String? = null
)

data class MangaResponse(
    @SerializedName("data")
    val data: List<Manga>? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("success")
    val success: Boolean = false
)

data class MangaDetailResponse(
    @SerializedName("data")
    val data: Manga? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("success")
    val success: Boolean = false
)

