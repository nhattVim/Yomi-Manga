package com.example.yomi_manga.di

import android.content.Context
import androidx.room.Room
import com.example.yomi_manga.data.api.ApiClient
import com.example.yomi_manga.data.api.OtruyenApiService
import com.example.yomi_manga.data.local.AppDatabase
import com.example.yomi_manga.data.repository.AuthRepository
import com.example.yomi_manga.data.repository.DownloadRepository
import com.example.yomi_manga.data.repository.MangaRepository
import com.example.yomi_manga.data.repository.UserRepository

object AppContainer {
    private var database: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "yomi_manga_db"
            ).build()
        }
        return database!!
    }

    val apiService: OtruyenApiService by lazy { ApiClient.apiService }
    
    val userRepository: UserRepository by lazy { UserRepository() }
    val authRepository: AuthRepository by lazy { AuthRepository(userRepository) }
    val mangaRepository: MangaRepository by lazy { MangaRepository() }

    private var _downloadRepository: DownloadRepository? = null
    fun provideDownloadRepository(context: Context): DownloadRepository {
        if (_downloadRepository == null) {
            val db = provideDatabase(context)
            _downloadRepository = DownloadRepository(
                context.applicationContext,
                db.downloadedChapterDao(),
                db.downloadedMangaDao()
            )
        }
        return _downloadRepository!!
    }
}