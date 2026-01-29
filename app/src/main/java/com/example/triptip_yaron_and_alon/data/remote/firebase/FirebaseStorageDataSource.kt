package com.example.triptip_yaron_and_alon.data.remote.firebase

import android.content.Context
import android.net.Uri
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Data source for image storage operations.
 * 
 * **Note:** Using local file storage for development/testing.
 * Images are saved to the app's external files directory.
 * 
 * For production, this can be switched to Firebase Storage by:
 * 1. Replacing local file operations with Firebase Storage operations
 * 2. Uploading to Firebase Storage bucket
 * 3. Getting download URLs from Firebase Storage
 * 
 * All methods are asynchronous and use coroutines.
 */
class FirebaseStorageDataSource(
    private val context: Context
) {
    
    /**
     * Upload an image to local storage.
     * Returns Flow<Result<String>> with the file path.
     * 
     * @param uri The URI of the image to upload
     * @param storagePath The storage path (e.g., "post_images" or "profile_images")
     * @return Flow emitting Result with the file path (e.g., "/storage/.../Pictures/posts/user123/image.jpg")
     */
    fun uploadImage(uri: Uri, storagePath: String): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            val filePath = withContext(Dispatchers.IO) {
                saveImageToLocalStorage(uri, storagePath)
            }
            emit(Result.Success(filePath))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message ?: "Failed to upload image"))
        }
    }
    
    /**
     * Delete an image from local storage.
     * Returns Result<Unit> indicating success or failure.
     * 
     * @param imageUrl The file path of the image to delete
     */
    suspend fun deleteImage(imageUrl: String?): Result<Unit> {
        if (imageUrl.isNullOrEmpty()) {
            return Result.Success(Unit)
        }
        
        return try {
            withContext(Dispatchers.IO) {
                val file = File(imageUrl)
                if (file.exists()) {
                    file.delete()
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to delete image")
        }
    }
    
    /**
     * Save image to local storage.
     * Returns the file path of the saved image.
     */
    private suspend fun saveImageToLocalStorage(uri: Uri, storagePath: String): String {
        val imagesDir = File(context.getExternalFilesDir(null), storagePath)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        // Generate unique filename
        val fileName = "${UUID.randomUUID()}.jpg"
        val imageFile = File(imagesDir, fileName)
        
        // Copy image from URI to file
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(imageFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("Could not open input stream from URI")
        
        return imageFile.absolutePath
    }
    
    /**
     * Get a File object from a file path.
     * Useful for loading images with Coil/Picasso.
     */
    fun getImageFile(imagePath: String?): File? {
        if (imagePath.isNullOrEmpty()) return null
        val file = File(imagePath)
        return if (file.exists()) file else null
    }
    
    /**
     * Get a Uri from a file path.
     * Useful for loading images with Coil/Picasso.
     */
    fun getImageUri(imagePath: String?): Uri? {
        val file = getImageFile(imagePath) ?: return null
        return Uri.fromFile(file)
    }
}

