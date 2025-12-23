package com.example.yomi_manga.data.repository

import android.util.Log
import com.example.yomi_manga.core.constants.AppConstants
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val userRepository: UserRepository = UserRepository()
) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            Log.d(AppConstants.TAG_AUTH, "Signing in with Google, idToken length: ${idToken.length}")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            
            result.user?.let { user ->
                firestoreScope.launch {
                    try {
                        userRepository.saveUser(user)
                    } catch (e: Exception) {
                        Log.e(AppConstants.TAG_AUTH, "Error saving user to Firestore", e)
                    }
                }
                Result.success(user)
            } ?: Result.failure(Exception("Đăng nhập thất bại"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            user?.delete()?.await()
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.e(AppConstants.TAG_AUTH, "Recent login required", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_AUTH, "Delete account failed", e)
            Result.failure(e)
        }
    }

    suspend fun reauthenticate(idToken: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user?.reauthenticate(credential)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
