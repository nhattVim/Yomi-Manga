package com.example.yomi_manga.data.api

import com.example.yomi_manga.data.model.CategoryListRespone
import com.example.yomi_manga.data.model.ChapterResponse
import com.example.yomi_manga.data.model.MangaDetailResponse
import com.example.yomi_manga.data.model.MangaListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface OtruyenApiService {

    @GET("home")
    suspend fun getHomeManga(): Response<MangaListResponse>

    @GET("the-loai")
    suspend fun getCategoryList(): Response<CategoryListRespone>

    @GET("danh-sach/{slug}")
    suspend fun getMangaList(
        @Path("slug") slug: String = "truyen-moi",
        @Query("page") page: Int = 1
    ): Response<MangaListResponse>

    @GET("truyen-tranh/{slug}")
    suspend fun getMangaDetail(
        @Path("slug") slug: String
    ): Response<MangaDetailResponse>

    @GET("tim-kiem")
    suspend fun searchManga(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1
    ): Response<MangaListResponse>

    @GET("the-loai/{slug}")
    suspend fun getMangaByCategory(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1
    ): Response<MangaListResponse>

    @GET
    suspend fun getChapter(@Url url: String): Response<ChapterResponse>
}
