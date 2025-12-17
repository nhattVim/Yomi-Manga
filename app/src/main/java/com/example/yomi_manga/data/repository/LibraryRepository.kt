package com.example.yomi_manga.data.repository

import android.util.Log
import com.example.yomi_manga.core.constants.AppConstants
import com.example.yomi_manga.data.model.FavoriteManga
import com.example.yomi_manga.data.model.ReadingHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LibraryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String?
        get() = auth.currentUser?.uid

    // --- Favorites ---

    fun getFavoriteMangaList(): Flow<List<FavoriteManga>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            Log.w(AppConstants.TAG_USER, "Favorites: User not logged in")
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
            .collection(AppConstants.FIRESTORE_FAVORITES_FIELD)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(AppConstants.TAG_USER, "Error fetching favorites", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val favorites = snapshot.toObjects(FavoriteManga::class.java)
                    trySend(favorites)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addFavorite(manga: FavoriteManga) {
        val uid = userId ?: return
        Log.d(AppConstants.TAG_USER, "Adding favorite: ${manga.mangaId} for user $uid")
        firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
            .collection(AppConstants.FIRESTORE_FAVORITES_FIELD)
            .document(manga.mangaId)
            .set(manga)
            .await()
    }

    suspend fun removeFavorite(mangaId: String) {
        val uid = userId ?: return
        Log.d(AppConstants.TAG_USER, "Removing favorite: $mangaId for user $uid")
        firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
            .collection(AppConstants.FIRESTORE_FAVORITES_FIELD)
            .document(mangaId)
            .delete()
            .await()
    }

    suspend fun isFavorite(mangaId: String): Boolean {
        val uid = userId ?: return false
        val doc = firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
            .collection(AppConstants.FIRESTORE_FAVORITES_FIELD)
            .document(mangaId)
            .get()
            .await()
        return doc.exists()
    }

    // --- Reading History ---

    fun getReadingHistory(): Flow<List<ReadingHistory>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            Log.w(AppConstants.TAG_USER, "History: User not logged in")
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
            .collection(AppConstants.FIRESTORE_HISTORY_FIELD)
            .orderBy("readAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(AppConstants.TAG_USER, "Error fetching history", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val history = snapshot.toObjects(ReadingHistory::class.java)
                    trySend(history)
                }
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun getReadingHistoryForManga(mangaId: String): ReadingHistory? {
        val uid = userId ?: return null
        return try {
            val doc = firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
                .collection(AppConstants.FIRESTORE_HISTORY_FIELD)
                .document(mangaId)
                .get()
                .await()
            doc.toObject(ReadingHistory::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun addToHistory(history: ReadingHistory) {
        val uid = userId ?: return
        val historyCollection = firestore.collection(AppConstants.FIRESTORE_USERS_COLLECTION).document(uid)
            .collection(AppConstants.FIRESTORE_HISTORY_FIELD)
            
        Log.d(AppConstants.TAG_USER, "Adding to history: ${history.mangaId} for user $uid")
        try {
            // Check if document exists to decide whether to check limit
            val docRef = historyCollection.document(history.mangaId)
            val docSnapshot = docRef.get().await()
            
            if (docSnapshot.exists()) {
                // If exists, just update (timestamp will be updated in the object)
                docRef.set(history).await()
            } else {
                // New entry, check limit
                val countSnapshot = historyCollection.count().get(AggregateSource.SERVER).await()
                if (countSnapshot.count >= AppConstants.MAX_READING_HISTORY) {
                    // Delete oldest
                    val oldestDocs = historyCollection.orderBy("readAt", Query.Direction.ASCENDING).limit(1).get().await()
                    for (doc in oldestDocs) {
                        doc.reference.delete().await()
                    }
                }
                docRef.set(history).await()
            }
            Log.d(AppConstants.TAG_USER, "History added successfully.")
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_USER, "Error adding to history", e)
        }
    }
}
