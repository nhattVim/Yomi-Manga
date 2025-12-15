package com.example.yomi_manga.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.yomi_manga.data.model.DownloadedChapter
import com.example.yomi_manga.data.model.DownloadedManga

@Database(entities = [DownloadedChapter::class, DownloadedManga::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedChapterDao(): DownloadedChapterDao
    abstract fun downloadedMangaDao(): DownloadedMangaDao
}