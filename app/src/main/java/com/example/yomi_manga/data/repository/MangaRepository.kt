package com.example.yomi_manga.data.repository

import com.example.yomi_manga.data.api.ApiClient
import com.example.yomi_manga.data.model.Category
import com.example.yomi_manga.data.model.Manga

class MangaRepository {
    private val apiService = ApiClient.apiService
    private var imageDomain: String = "https://img.otruyenapi.com/uploads/comics/"

    suspend fun getMangaList(slug: String, page: Int = 1): Result<List<Manga>> {
        return try {
            val response = apiService.getMangaList(slug, page)
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

    suspend fun getCategoryList(): Result<List<Category>> {
        return try {
            val response = apiService.getCategoryList()
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

    suspend fun getChapterPages(chapterApiUrl: String): Result<List<com.example.yomi_manga.data.model.ChapterImage>> {
        return try {
            val response = apiService.getChapter(chapterApiUrl)
            if (response.isSuccessful && response.body()?.status == "success") {
                val chapterData = response.body()?.data
                val domainCdn = chapterData?.domainCdn
                val chapterItem = chapterData?.item
                
                // We need to return the full URLs constructed, but since ChapterImage only has filename,
                // we might need to return a new object or list of strings.
                // However, DownloadRepository logic I wrote earlier expects full URLs if I recall correctly
                // or I constructed it inside ViewModel.
                
                // Let's see what I did in ViewModel:
                // imageUrls = pages.map { it.image_file ?: "" }.filter { it.isNotEmpty() }.map { 
                //     if (it.startsWith("http")) it else "${repository.getImageDomain()}/$it"
                // }
                
                // But wait, `getChapterPages` returns `Result<List<ChapterImage>>`?
                // The `ChapterItem` logic `getImageUrls(domainCdn)` already constructs full URLs.
                // So I should probably just return `List<String>` from here to keep it simple for ViewModel.

                chapterItem?.getImageUrls(domainCdn) ?: emptyList()
                
                // Return as ChapterImage with full URL in imageFile? 
                // No, better to return List<String> or List<ChapterImage> where I can store full URL.
                // But ChapterImage is data class. 
                
                // Let's return List<ChapterImage> but use a workaround or just change signature to return List<String> which is easier.
                // But wait, my ViewModel code `pages.map { it.image_file }` implies I return objects.
                
                // Let's look at `ChapterImage` class again.
                // data class ChapterImage(val imagePage: Int, val imageFile: String)
                
                // If I use `getImageUrls`, I get strings.
                // I will change signature to return List<String> (image URLs) directly.
                
                // But wait, ViewModel code:
                // val chapterContent = repository.getChapterPages(chapterApiData) 
                // chapterContent.fold( onSuccess = { pages -> ... } )
                
                // If I return List<String>, then `pages` is List<String>.
                // ViewModel: `pages.map { it.image_file ... }` -> this will break if pages is List<String>.
                
                // I should update ViewModel or make Repository compatible.
                // Updating ViewModel is safer as I can see what I wrote.
                // But let's check `MangaViewModel.kt` content again to be sure.
                
                // In previous turn I wrote:
                // chapterContent.fold(
                //      onSuccess = { pages -> 
                //          repo.downloadChapter(..., imageUrls = pages.map { it.image_file ?: "" } ... )
                
                // So ViewModel expects objects with `image_file`.
                
                // So I should return List<ChapterImage> here.
                // But I also need the domain info to construct full URL later in ViewModel?
                // Or I can update `imageFile` to be full URL here.
                
                val baseUrl = domainCdn?.trimEnd('/') ?: ""
                val path = chapterItem?.chapterPath?.trimStart('/') ?: ""
                
                val updatedImages = chapterItem?.chapterImage?.map { img ->
                     img.copy(imageFile = "$baseUrl/$path/${img.imageFile}")
                } ?: emptyList()
                
                Result.success(updatedImages)
            } else {
                Result.failure(Exception(response.message() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getImageDomain(): String {
         return imageDomain
    }
}
