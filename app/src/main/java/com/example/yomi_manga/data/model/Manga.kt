package com.example.yomi_manga.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: T? = null
)

data class Category(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String
)

data class CategoryListData(
    @SerializedName("items")
    val items: List<Category>? = null,
)

data class ChapterLatest(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("chapter_name")
    val chapterName: String,
    @SerializedName("chapter_title")
    val chapterTitle: String?,
    @SerializedName("chapter_api_data")
    val chapterApiData: String
)

data class ChapterData(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("chapter_name")
    val chapterName: String,
    @SerializedName("chapter_title")
    val chapterTitle: String?,
    @SerializedName("chapter_api_data")
    val chapterApiData: String
)

data class ChapterServer(
    @SerializedName("server_name")
    val serverName: String,
    @SerializedName("server_data")
    val serverData: List<ChapterData>
)

data class Manga(
    @SerializedName("_id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("origin_name")
    val originName: List<String>? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("thumb_url")
    val thumbUrl: String? = null,
    @SerializedName("sub_docquyen")
    val subDocquyen: Boolean = false,
    @SerializedName("author")
    val author: List<String>? = null,
    @SerializedName("category")
    val category: List<Category>? = null,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("chaptersLatest")
    val chaptersLatest: List<ChapterLatest>? = null,
    @SerializedName("chapters")
    val chapters: List<ChapterServer>? = null
) {
    val title: String get() = name
    val cover: String? get() = thumbUrl?.let { "https://img.otruyenapi.com/uploads/comics/$it" }
    val description: String? get() = content
    val genres: List<String>? get() = category?.map { it.name }
    val authorName: String? get() = author?.joinToString(", ")
}

data class MangaListData(
    @SerializedName("items")
    val items: List<Manga>? = null,
    @SerializedName("params")
    val params: PaginationParams? = null,
    @SerializedName("seoOnPage")
    val seoOnPage: SeoOnPage? = null,
    @SerializedName("APP_DOMAIN_CDN_IMAGE")
    val appDomainCdnImage: String? = null
)

data class PaginationParams(
    @SerializedName("pagination")
    val pagination: Pagination? = null
)

data class Pagination(
    @SerializedName("totalItems")
    val totalItems: Int? = null,
    @SerializedName("totalItemsPerPage")
    val totalItemsPerPage: Int? = null,
    @SerializedName("currentPage")
    val currentPage: Int? = null,
    @SerializedName("pageRanges")
    val pageRanges: Int? = null
)

data class SeoOnPage(
    @SerializedName("titleHead")
    val titleHead: String? = null,
    @SerializedName("descriptionHead")
    val descriptionHead: String? = null,
    @SerializedName("og_image")
    val ogImage: List<String>? = null
)

data class MangaDetailData(
    @SerializedName("item")
    val item: Manga? = null,
    @SerializedName("APP_DOMAIN_CDN_IMAGE")
    val appDomainCdnImage: String? = null,
    @SerializedName("seoOnPage")
    val seoOnPage: SeoOnPage? = null
)

data class ChapterResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: ChapterDataResponse? = null
)

data class ChapterDataResponse(
    @SerializedName("domain_cdn")
    val domainCdn: String? = null,
    @SerializedName("item")
    val item: ChapterItem? = null
)

data class ChapterItem(
    @SerializedName("_id")
    val id: String? = null,
    @SerializedName("comic_name")
    val comicName: String? = null,
    @SerializedName("chapter_name")
    val chapterName: String? = null,
    @SerializedName("chapter_title")
    val chapterTitle: String? = null,
    @SerializedName("chapter_path")
    val chapterPath: String? = null,
    @SerializedName("chapter_image")
    val chapterImage: List<ChapterImage>? = null
) {
    fun getImageUrls(domainCdn: String?): List<String> {
        if (domainCdn == null || chapterPath == null || chapterImage == null) {
            return emptyList()
        }
        val baseUrl = domainCdn.trimEnd('/')
        val path = chapterPath.trimStart('/')
        return chapterImage.sortedBy { it.imagePage }.map { image ->
            "$baseUrl/$path/${image.imageFile}"
        }
    }
}

data class ChapterImage(
    @SerializedName("image_page")
    val imagePage: Int,
    @SerializedName("image_file")
    val imageFile: String
)

typealias MangaListResponse = ApiResponse<MangaListData>
typealias MangaDetailResponse = ApiResponse<MangaDetailData>
typealias CategoryListRespone = ApiResponse<CategoryListData>
