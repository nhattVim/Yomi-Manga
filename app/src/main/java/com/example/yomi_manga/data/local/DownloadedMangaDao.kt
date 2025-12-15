package com.example.yomi_manga.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.yomi_manga.data.model.DownloadedManga
import com.example.yomi_manga.data.model.MangaAndChapters
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedMangaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedManga(manga: DownloadedManga)

    @Transaction
    @Query("SELECT * FROM downloaded_manga ORDER BY lastDownloaded DESC")
    fun getDownloadedMangaAndChapters(): Flow<List<MangaAndChapters>>

    @Query("SELECT * FROM downloaded_manga WHERE mangaId = :mangaId")
    suspend fun getDownloadedManga(mangaId: String): DownloadedManga?
    
    @Delete
    suspend fun deleteDownloadedManga(manga: DownloadedManga)
}