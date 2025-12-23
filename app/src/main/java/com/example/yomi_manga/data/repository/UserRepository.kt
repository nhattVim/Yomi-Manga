package com.example.yomi_manga.data.repository

import android.util.Log
import com.example.yomi_manga.core.constants.AppConstants
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
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa sạch dữ liệu user trong Firestore
     */
    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Log.d(AppConstants.TAG_USER, "User document deleted: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_USER, "Error deleting user document", e)
            Result.failure(e)
        }
    }
}
