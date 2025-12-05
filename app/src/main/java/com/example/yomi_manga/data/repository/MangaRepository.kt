package com.example.yomi_manga.data.repository

import com.example.yomi_manga.data.api.ApiClient
import com.example.yomi_manga.data.model.Manga
import com.example.yomi_manga.data.model.MangaDetailResponse
import com.example.yomi_manga.data.model.MangaResponse

class MangaRepository {
    private val apiService = ApiClient.apiService
    
    suspend fun getMangaList(page: Int = 1, limit: Int = 20): Result<List<Manga>> {
        return try {
            val response = apiService.getMangaList(page, limit)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMangaDetail(id: String): Result<Manga> {
        return try {
            val response = apiService.getMangaDetail(id)
            if (response.isSuccessful && response.body()?.success == true) {
                val manga = response.body()?.data
                if (manga != null) {
                    Result.success(manga)
                } else {
                    Result.failure(Exception("Manga not found"))
                }
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchManga(query: String, page: Int = 1): Result<List<Manga>> {
        return try {
            val response = apiService.searchManga(query, page)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPopularManga(page: Int = 1, limit: Int = 20): Result<List<Manga>> {
        return try {
            val response = apiService.getPopularManga(page, limit)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLatestManga(page: Int = 1, limit: Int = 20): Result<List<Manga>> {
        return try {
            val response = apiService.getLatestManga(page, limit)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

