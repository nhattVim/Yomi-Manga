package com.example.yomi_manga.data.api

import com.example.yomi_manga.data.model.MangaDetailResponse
import com.example.yomi_manga.data.model.MangaResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OtruyenApiService {
    
    @GET("manga")
    suspend fun getMangaList(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<MangaResponse>
    
    @GET("manga/{id}")
    suspend fun getMangaDetail(
        @Path("id") id: String
    ): Response<MangaDetailResponse>
    
    @GET("manga/search")
    suspend fun searchManga(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): Response<MangaResponse>
    
    @GET("manga/popular")
    suspend fun getPopularManga(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<MangaResponse>
    
    @GET("manga/latest")
    suspend fun getLatestManga(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<MangaResponse>
}

