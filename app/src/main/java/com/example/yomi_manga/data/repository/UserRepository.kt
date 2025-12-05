package com.example.yomi_manga.data.repository

import android.util.Log
import com.example.yomi_manga.core.constants.AppConstants
import com.example.yomi_manga.data.model.ReadingHistory
import com.example.yomi_manga.data.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION)
    
    /**
     * Lưu hoặc cập nhật thông tin user vào Firestore
     */
    suspend fun saveUser(user: FirebaseUser): Result<User> {
        return try {
            Log.d(AppConstants.TAG_USER, "Saving user: ${user.uid}")
            val userData = User(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            
            usersCollection.document(user.uid).set(
                mapOf(
                    "uid" to userData.uid,
                    "email" to userData.email,
                    "displayName" to userData.displayName,
                    "photoUrl" to userData.photoUrl,
                    "createdAt" to userData.createdAt
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Log.d(AppConstants.TAG_USER, "User saved successfully")
            Result.success(userData)
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_USER, "Error saving user", e)
            Result.failure(e)
        }
    }
    
    /**
     * Lấy thông tin user từ Firestore
     */
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val data = document.data ?: emptyMap()
                val user = User(
                    uid = data["uid"] as? String ?: uid,
                    email = data["email"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                    favoriteManga = (data[AppConstants.FIRESTORE_FAVORITES_FIELD] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    readingHistory = parseReadingHistory(data[AppConstants.FIRESTORE_HISTORY_FIELD])
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseReadingHistory(data: Any?): List<ReadingHistory> {
        if (data !is List<*>) return emptyList()
        return data.mapNotNull { item ->
            if (item is Map<*, *>) {
                try {
                    ReadingHistory(
                        mangaId = (item["mangaId"] as? String) ?: "",
                        mangaTitle = (item["mangaTitle"] as? String) ?: "",
                        chapterId = (item["chapterId"] as? String) ?: "",
                        chapterTitle = (item["chapterTitle"] as? String) ?: "",
                        lastReadAt = (item["lastReadAt"] as? Long) ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    
    /**
     * Thêm manga vào danh sách yêu thích
     */
    suspend fun addFavoriteManga(uid: String, mangaId: String): Result<Unit> {
        return try {
            val userRef = usersCollection.document(uid)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                @Suppress("UNCHECKED_CAST")
                val currentFavorites = (snapshot.get(AppConstants.FIRESTORE_FAVORITES_FIELD) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (!currentFavorites.contains(mangaId)) {
                    transaction.update(userRef, AppConstants.FIRESTORE_FAVORITES_FIELD, currentFavorites + mangaId)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Xóa manga khỏi danh sách yêu thích
     */
    suspend fun removeFavoriteManga(uid: String, mangaId: String): Result<Unit> {
        return try {
            val userRef = usersCollection.document(uid)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                @Suppress("UNCHECKED_CAST")
                val currentFavorites = (snapshot.get(AppConstants.FIRESTORE_FAVORITES_FIELD) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                transaction.update(userRef, AppConstants.FIRESTORE_FAVORITES_FIELD, currentFavorites - mangaId)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Lưu lịch sử đọc
     */
    suspend fun saveReadingHistory(
        uid: String,
        mangaId: String,
        mangaTitle: String,
        chapterId: String,
        chapterTitle: String
    ): Result<Unit> {
        return try {
            val history = ReadingHistory(
                mangaId = mangaId,
                mangaTitle = mangaTitle,
                chapterId = chapterId,
                chapterTitle = chapterTitle,
                lastReadAt = System.currentTimeMillis()
            )
            
            val userRef = usersCollection.document(uid)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                @Suppress("UNCHECKED_CAST")
                val currentHistory = (snapshot.get(AppConstants.FIRESTORE_HISTORY_FIELD) as? List<*>)?.mapNotNull { 
                    it as? Map<String, Any> 
                } ?: emptyList()
                
                val filteredHistory = currentHistory.filterNot { 
                    (it["mangaId"] as? String) == mangaId 
                }
                
                val newHistory = (listOf(
                    mapOf(
                        "mangaId" to history.mangaId,
                        "mangaTitle" to history.mangaTitle,
                        "chapterId" to history.chapterId,
                        "chapterTitle" to history.chapterTitle,
                        "lastReadAt" to history.lastReadAt
                    )
                ) + filteredHistory).take(AppConstants.MAX_READING_HISTORY)
                
                transaction.update(userRef, AppConstants.FIRESTORE_HISTORY_FIELD, newHistory)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

