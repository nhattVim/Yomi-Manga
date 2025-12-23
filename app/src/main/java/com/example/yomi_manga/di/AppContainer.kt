package com.example.yomi_manga.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.yomi_manga.data.api.ApiClient
import com.example.yomi_manga.data.api.OtruyenApiService
import com.example.yomi_manga.data.local.AppDatabase
import com.example.yomi_manga.data.repository.AuthRepository
import com.example.yomi_manga.data.repository.DownloadRepository
import com.example.yomi_manga.data.repository.LibraryRepository
import com.example.yomi_manga.data.repository.MangaRepository
import com.example.yomi_manga.data.repository.SettingsRepository
import com.example.yomi_manga.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppContainer {
    private var database: AppDatabase? = null
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    // Coil ImageLoader with improved caching
    private var imageLoader: ImageLoader? = null
    fun provideImageLoader(context: Context): ImageLoader {
        if (imageLoader == null) {
            imageLoader = ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .respectCacheHeaders(false) // Force cache if headers say otherwise
                .build()
        }
        return imageLoader!!
    }

    val apiService: OtruyenApiService by lazy { ApiClient.apiService }
    
    val userRepository: UserRepository by lazy { UserRepository() }
    val authRepository: AuthRepository by lazy { AuthRepository(userRepository) }
    val mangaRepository: MangaRepository by lazy { MangaRepository() }
    val libraryRepository: LibraryRepository by lazy { LibraryRepository() }

    private var _downloadRepository: DownloadRepository? = null
    fun provideDownloadRepository(context: Context): DownloadRepository {
        if (_downloadRepository == null) {
            val db = provideDatabase(context)
            _downloadRepository = DownloadRepository(
                context.applicationContext,
                db.downloadedChapterDao(),
                db.downloadedMangaDao(),
                applicationScope
            )
        }
        return _downloadRepository!!
    }

    private var _settingsRepository: SettingsRepository? = null
    fun provideSettingsRepository(context: Context): SettingsRepository {
        if (_settingsRepository == null) {
            _settingsRepository = SettingsRepository(context.applicationContext)
        }
        return _settingsRepository!!
    }
}
