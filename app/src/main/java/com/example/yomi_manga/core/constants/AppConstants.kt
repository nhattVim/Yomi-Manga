package com.example.yomi_manga.core.constants

object AppConstants {
    const val API_BASE_URL = "https://otruyenapi.com/v1/api/"

    const val FIRESTORE_USERS_COLLECTION = "users"
    const val FIRESTORE_FAVORITES_FIELD = "favorites"
    const val FIRESTORE_HISTORY_FIELD = "history"
    const val MAX_READING_HISTORY = 20
    
    const val NAV_ARG_MANGA_ID = "mangaId"
    const val NAV_ARG_MANGA_SLUG = "mangaSlug"
    const val NAV_ARG_CHAPTER_ID = "chapterId"
    
    const val TAG_AUTH = "AuthRepository"
    const val TAG_MANGA = "MangaRepository"
    const val TAG_USER = "UserRepository"
    const val TAG_LOGIN = "LoginScreen"
    const val TAG_FIREBASE = "FirebaseConfig"
}
