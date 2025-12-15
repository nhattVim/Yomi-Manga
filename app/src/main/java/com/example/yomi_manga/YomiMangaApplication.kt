package com.example.yomi_manga

import android.app.Application
import com.example.yomi_manga.di.AppContainer
import com.google.firebase.FirebaseApp

class YomiMangaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AppContainer.provideDatabase(this)
        AppContainer.provideDownloadRepository(this)
    }
}
