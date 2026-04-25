package com.example.triptip_yaron_and_alon.data.remote.firebase

import android.content.Context
import android.net.Uri
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirebaseStorageDataSource(
    private val context: Context
) {

    // Explicitly reference the storage bucket to avoid SDK auto-detection issues
    // with the newer .firebasestorage.app domain.
    private val storage = FirebaseStorage.getInstance("gs://triptip-97085.firebasestorage.app")

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
        } catch (_: Exception) {
            // Treat delete failures as non-fatal (old URL may already be gone or not a Storage URL)
            Result.Success(Unit)
        }
    }

    private suspend fun uploadToFirebase(uri: Uri, storagePath: String): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("$storagePath/$fileName")

        // Read the image bytes through ContentResolver so it works for
        // both content:// URIs (PickVisualMedia) and file:// URIs.
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArrayOutputStream()
            stream.copyTo(buffer)
            buffer.toByteArray()
        } ?: throw IllegalStateException("Could not read image from URI: $uri")

        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }
}
