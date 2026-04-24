package com.example.triptip_yaron_and_alon.data.remote.firebase

import android.content.Context
import android.net.Uri
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageDataSource(
    @Suppress("UnusedPrivateMember") private val context: Context
) {

    private val storage = Firebase.storage

    /**
     * Upload an image to Firebase Storage.
     * Returns Flow<Result<String>> with the public HTTPS download URL.
     */
    fun uploadImage(uri: Uri, storagePath: String): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            val url = uploadToFirebase(uri, storagePath)
            emit(Result.Success(url))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message ?: "Failed to upload image"))
        }
    }

    /**
     * Upload an image to Firebase Storage (suspend version).
     * Returns Result<String> with the public HTTPS download URL.
     */
    suspend fun uploadImageSync(uri: Uri, storagePath: String): Result<String> {
        return try {
            val url = uploadToFirebase(uri, storagePath)
            Result.Success(url)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to upload image")
        }
    }

    /**
     * Delete an image from Firebase Storage by its download URL.
     * Silently succeeds if the URL is empty or not a Firebase Storage URL.
     */
    suspend fun deleteImage(imageUrl: String?): Result<Unit> {
        if (imageUrl.isNullOrEmpty()) return Result.Success(Unit)
        return try {
            storage.getReferenceFromUrl(imageUrl).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            // Treat delete failures as non-fatal (the old URL may already be gone)
            Result.Success(Unit)
        }
    }

    private suspend fun uploadToFirebase(uri: Uri, storagePath: String): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("$storagePath/$fileName")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
