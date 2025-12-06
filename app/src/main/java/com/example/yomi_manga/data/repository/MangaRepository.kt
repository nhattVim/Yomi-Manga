package com.example.yomi_manga.data.repository

import com.example.yomi_manga.data.api.ApiClient
import com.example.yomi_manga.data.model.Manga

class MangaRepository {
    private val apiService = ApiClient.apiService
    
    suspend fun getMangaList(page: Int = 1): Result<List<Manga>> {
        return try {
            val response = apiService.getLatestManga(page)
            if (response.isSuccessful && response.body()?.status == "success") {
                val items = response.body()?.data?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getHomeManga(): Result<List<Manga>> {
        return try {
            val response = apiService.getHomeManga()
            if (response.isSuccessful && response.body()?.status == "success") {
                val items = response.body()?.data?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMangaDetail(slug: String): Result<Manga> {
        return try {
            val response = apiService.getMangaDetail(slug)
            if (response.isSuccessful && response.body()?.status == "success") {
                val manga = response.body()?.data?.item
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
            if (response.isSuccessful && response.body()?.status == "success") {
                val items = response.body()?.data?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPopularManga(page: Int = 1): Result<List<Manga>> {
        return try {
            val response = apiService.getHomeManga()
            if (response.isSuccessful && response.body()?.status == "success") {
                val items = response.body()?.data?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMangaByCategory(categorySlug: String, page: Int = 1): Result<List<Manga>> {
        return try {
            val response = apiService.getMangaByCategory(categorySlug, page)
            if (response.isSuccessful && response.body()?.status == "success") {
                val items = response.body()?.data?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChapterImages(chapterApiUrl: String): Result<List<String>> {
        return try {
            val response = apiService.getChapter(chapterApiUrl)
            if (response.isSuccessful && response.body()?.status == "success") {
                val chapterData = response.body()?.data
                val domainCdn = chapterData?.domainCdn
                val chapterItem = chapterData?.item
                val imageUrls = chapterItem?.getImageUrls(domainCdn) ?: emptyList()
                Result.success(imageUrls)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
