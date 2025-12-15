package com.example.yomi_manga.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.yomi_manga.data.local.DownloadedChapterDao
import com.example.yomi_manga.data.local.DownloadedMangaDao
import com.example.yomi_manga.data.model.DownloadedChapter
import com.example.yomi_manga.data.model.DownloadedManga
import com.example.yomi_manga.data.model.Manga
import com.example.yomi_manga.data.model.MangaAndChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class DownloadRepository(
    private val context: Context,
    private val downloadedChapterDao: DownloadedChapterDao,
    private val downloadedMangaDao: DownloadedMangaDao
) {
    fun getAllDownloadedManga(): Flow<List<MangaAndChapters>> {
        return downloadedMangaDao.getDownloadedMangaAndChapters()
    }

    fun getDownloadedChapters(mangaId: String): Flow<List<DownloadedChapter>> {
        return downloadedChapterDao.getDownloadedChaptersForManga(mangaId)
    }

    suspend fun isChapterDownloaded(chapterId: String): Boolean {
        return downloadedChapterDao.getDownloadedChapter(chapterId) != null
    }
    
    suspend fun getDownloadedChapter(chapterId: String): DownloadedChapter? {
        return downloadedChapterDao.getDownloadedChapter(chapterId)
    }

    suspend fun downloadChapter(
        manga: Manga,
        chapterId: String,
        chapterTitle: String,
        chapterNumber: Float,
        imageUrls: List<String>
    ) {
        withContext(Dispatchers.IO) {
            // Save Manga info if not exists
            val existingManga = downloadedMangaDao.getDownloadedManga(manga.id ?: "")
            if (existingManga == null) {
                val downloadedManga = DownloadedManga(
                    mangaId = manga.id ?: "",
                    title = manga.title,
                    slug = manga.slug,
                    coverUrl = manga.cover ?: "",
                    author = manga.authorName,
                    lastDownloaded = Date().time
                )
                downloadedMangaDao.insertDownloadedManga(downloadedManga)
            }

            val imagePaths = mutableListOf<String>()
            val chapterDir = File(context.filesDir, "manga/${manga.id}/$chapterId")
            if (!chapterDir.exists()) {
                chapterDir.mkdirs()
            }

            val imageLoader = ImageLoader(context)

            for ((index, imageUrl) in imageUrls.withIndex()) {
                val fileName = "page_$index.jpg"
                val file = File(chapterDir, fileName)
                
                if (file.exists()) {
                    imagePaths.add(file.absolutePath)
                    continue
                }

                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    imagePaths.add(file.absolutePath)
                }
            }

            val downloadedChapter = DownloadedChapter(
                chapterId = chapterId,
                mangaId = manga.id ?: "",
                chapterTitle = chapterTitle,
                chapterNumber = chapterNumber,
                downloadDate = Date().time,
                imagePaths = imagePaths
            )
            downloadedChapterDao.insertDownloadedChapter(downloadedChapter)
        }
    }

    suspend fun deleteChapter(chapter: DownloadedChapter) {
        withContext(Dispatchers.IO) {
            val chapterDir = File(context.filesDir, "manga/${chapter.mangaId}/${chapter.chapterId}")
            if (chapterDir.exists()) {
                chapterDir.deleteRecursively()
            }
            downloadedChapterDao.deleteDownloadedChapter(chapter)
            
            // Check if manga has any chapters left, if not delete manga
            val chapters = downloadedChapterDao.getDownloadedChaptersForMangaList(chapter.mangaId)
            if (chapters.isEmpty()) {
                val manga = downloadedMangaDao.getDownloadedManga(chapter.mangaId)
                if (manga != null) {
                    downloadedMangaDao.deleteDownloadedManga(manga)
                }
            }
        }
    }

    suspend fun deleteManga(manga: DownloadedManga) {
        withContext(Dispatchers.IO) {
            // Delete all chapters files
            val mangaDir = File(context.filesDir, "manga/${manga.mangaId}")
            if (mangaDir.exists()) {
                mangaDir.deleteRecursively()
            }
            
            // Delete from DB (Chapters will be deleted by cascade if configured, but here we manually delete or let Room handle if foreign keys set, but we didn't set FK yet. 
            // Since we didn't set FK with Cascade, we should delete chapters manually or rely on logic.
            // Let's delete chapters manually first to be safe or rely on `deleteDownloadedManga` and `deleteDownloadedChapter` calls? 
            // Actually, we should just delete everything for this manga from DB.
            
            // However, `downloadedChapterDao` doesn't have `deleteChaptersForManga`. Let's add it or iterate.
            val chapters = downloadedChapterDao.getDownloadedChaptersForMangaList(manga.mangaId)
            chapters.forEach { downloadedChapterDao.deleteDownloadedChapter(it) }
            
            downloadedMangaDao.deleteDownloadedManga(manga)
        }
    }
}