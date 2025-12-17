package com.example.yomi_manga.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
    // Removed favoriteManga and readingHistory lists as we now use sub-collections for scalability
)