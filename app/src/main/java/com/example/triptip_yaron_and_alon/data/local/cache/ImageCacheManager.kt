package com.example.triptip_yaron_and_alon.data.local.cache

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages image caching with cache-first strategy.
 * 
 * Strategy:
 * 1. Check if image exists locally (from Room path)
 * 2. If not, check if it's a remote URL and download it (future enhancement)
 * 3. Return local path for Coil to load, or null if image unavailable
 * 
 * This ensures that:
 * - Images are properly cached locally
 * - Image paths from Room are verified before use
 * - Missing files are handled gracefully
 * - No Firebase local persistence is used (only Room + local file storage)
 */
class ImageCacheManager(
    private val context: Context
) {
    
    /**
     * Get image path with cache-first strategy.
     * Verifies that the image file exists locally before returning the path.
     * 
     * @param imagePathOrUrl Path from Room (local file path) or remote URL
     * @return Flow<String?> with local file path, or null if image unavailable
     */
    fun getCachedImagePath(imagePathOrUrl: String?): Flow<String?> = flow {
        if (imagePathOrUrl.isNullOrBlank()) {
            emit(null)
            return@flow
        }
        
        val verifiedPath = withContext(Dispatchers.IO) {
            verifyImageExists(imagePathOrUrl)
        }
        
        emit(verifiedPath)
    }
    
    /**
     * Verify image file exists and return path or null.
     * This is the core cache-first check: ensure file exists before using it.
     * 
     * @param imagePath Path from Room database
     * @return Verified file path if exists, null otherwise
     */
    suspend fun verifyImageExists(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null
        
        return withContext(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                if (file.exists() && file.isFile && file.length() > 0) {
                    // File exists and is valid
                    imagePath
                } else {
                    // File doesn't exist or is invalid
                    null
                }
            } catch (e: Exception) {
                // Error accessing file
                null
            }
        }
    }
    
    /**
     * Check if image path is a remote URL.
     * Currently, images are stored locally, but this supports future remote URL handling.
     */
    private fun isRemoteUrl(path: String): Boolean {
        return path.startsWith("http://") || 
               path.startsWith("https://") ||
               path.startsWith("gs://") ||
               path.startsWith("firebase://")
    }
    
    /**
     * Get File object from image path if it exists.
     * Useful for Coil which can load from File objects.
     */
    suspend fun getImageFile(imagePath: String?): File? {
        if (imagePath.isNullOrBlank()) return null
        
        return withContext(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                if (file.exists() && file.isFile && file.length() > 0) {
                    file
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get Uri from image path if file exists.
     * Useful for loading images with Coil using Uri.
     */
    suspend fun getImageUri(imagePath: String?): Uri? {
        val file = getImageFile(imagePath) ?: return null
        return android.net.Uri.fromFile(file)
    }
}
