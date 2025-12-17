package com.example.yomi_manga

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.yomi_manga.di.AppContainer
import com.google.firebase.FirebaseApp

class YomiMangaApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AppContainer.provideDatabase(this)
        AppContainer.provideDownloadRepository(this)
    }

    override fun newImageLoader(): ImageLoader {
        return AppContainer.provideImageLoader(this)
    }
}
