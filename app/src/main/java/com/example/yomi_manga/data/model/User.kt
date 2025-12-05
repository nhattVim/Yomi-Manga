package com.example.yomi_manga.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val favoriteManga: List<String> = emptyList(),
    val readingHistory: List<ReadingHistory> = emptyList()
)

data class ReadingHistory(
    val mangaId: String = "",
    val mangaTitle: String = "",
    val chapterId: String = "",
    val chapterTitle: String = "",
    val lastReadAt: Long = System.currentTimeMillis()
)

