package com.example.yomi_manga.data.model

import com.google.firebase.Timestamp

data class FavoriteManga(
    val mangaId: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val slug: String = "",
    val addedAt: Timestamp = Timestamp.now()
)

data class ReadingHistory(
    val mangaId: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val slug: String = "",
    val chapterId: String = "",
    val chapterTitle: String = "",
    val readAt: Timestamp = Timestamp.now()
)