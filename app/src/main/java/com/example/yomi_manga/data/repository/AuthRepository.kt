package com.example.yomi_manga.data.repository

import android.util.Log
import com.example.yomi_manga.core.constants.AppConstants
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
            Log.d(AppConstants.TAG_AUTH, "Credential created, signing in...")
            val result = firebaseAuth.signInWithCredential(credential).await()
            Log.d(AppConstants.TAG_AUTH, "Sign in credential completed")
            
            result.user?.let { user ->
                Log.d(AppConstants.TAG_AUTH, "Sign in successful, user: ${user.email}, uid: ${user.uid}")
                
                firestoreScope.launch {
                    try {
                        userRepository.saveUser(user)
                        Log.d(AppConstants.TAG_AUTH, "User saved to Firestore")
                    } catch (e: Exception) {
                        Log.e(AppConstants.TAG_AUTH, "Error saving user to Firestore", e)
                    }
                }
                
                Result.success(user)
            } ?: run {
                Log.e(AppConstants.TAG_AUTH, "User is null after sign in")
                Result.failure(Exception("Đăng nhập thất bại: Không lấy được thông tin user"))
            }
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_AUTH, "Sign in failed", e)
            Log.e(AppConstants.TAG_AUTH, "Error message: ${e.message}")
            Log.e(AppConstants.TAG_AUTH, "Error cause: ${e.cause}")
            Result.failure(e)
        }
    }
    
    fun signOut() {
        firebaseAuth.signOut()
    }
}

