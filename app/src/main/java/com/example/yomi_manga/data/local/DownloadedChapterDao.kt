package com.example.yomi_manga.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.yomi_manga.data.model.DownloadedChapter
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedChapterDao {
    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadDate DESC")
    fun getAllDownloadedChapters(): Flow<List<DownloadedChapter>>

    @Query("SELECT * FROM downloaded_chapters WHERE mangaId = :mangaId ORDER BY chapterNumber DESC")
    fun getDownloadedChaptersForManga(mangaId: String): Flow<List<DownloadedChapter>>

    @Query("SELECT * FROM downloaded_chapters WHERE mangaId = :mangaId")
    suspend fun getDownloadedChaptersForMangaList(mangaId: String): List<DownloadedChapter>

    @Query("SELECT * FROM downloaded_chapters WHERE chapterId = :chapterId")
    suspend fun getDownloadedChapter(chapterId: String): DownloadedChapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedChapter(chapter: DownloadedChapter)

    @Delete
    suspend fun deleteDownloadedChapter(chapter: DownloadedChapter)
}