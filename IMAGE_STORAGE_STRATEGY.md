# Image Storage Strategy

## Current Approach: Local File Storage Fallback

For development and testing, we're using **local file storage** instead of Firebase Storage. Images are stored locally on the device and file paths are used as "URLs" in the database.

### How It Works

1. **Image Upload:**
   - User selects image from gallery/camera
   - Image is saved to app's internal storage (`context.filesDir` or `context.getExternalFilesDir()`)
   - File path is stored in Firestore as `imageUrl`
   - Path format: `/images/posts/{userId}/{timestamp}.jpg`

2. **Image Retrieval:**
   - When loading posts, we get the file path from Firestore
   - Load image from local storage using the path
   - Coil/Picasso handles loading from file paths automatically

3. **Benefits:**
   - ✅ No external service needed
   - ✅ Works offline
   - ✅ Good for testing
   - ✅ Fast (no network calls)
   - ✅ Easy to switch to Firebase Storage later

### Limitations

- ❌ Images don't sync across devices
- ❌ Images are lost if app is uninstalled
- ❌ Not suitable for production (users can't see each other's images)
- ❌ Storage limited to device storage

### Implementation Details

**FirebaseStorageDataSource.kt** will:
- Save images to `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)`
- Return file paths (e.g., `file:///storage/emulated/0/Android/data/com.example.triptip_yaron_and_alon/files/Pictures/posts/user123/image.jpg`)
- Store these paths in Firestore as `imageUrl`

**Image Loading:**
- Coil/Picasso can load from file paths directly
- Use `file://` URI scheme or `File` object

### Migration Path to Firebase Storage

When ready to enable Firebase Storage:

1. Enable Firebase Storage in Firebase Console
2. Update `FirebaseStorageDataSource.kt`:
   - Replace local file saving with Firebase Storage upload
   - Upload to Firebase Storage bucket
   - Return Firebase Storage download URL
   - Store Firebase URL in Firestore
3. No changes needed in:
   - Repository layer
   - UI layer
   - Image loading (Coil/Picasso works with both file paths and URLs)

### File Structure

```
app/
└── files/
    └── Pictures/
        ├── posts/
        │   └── {userId}/
        │       └── {timestamp}.jpg
        └── profiles/
            └── {userId}/
                └── profile.jpg
```

### Code Example (Preview)

```kotlin
// FirebaseStorageDataSource.kt (Local Storage Implementation)
class FirebaseStorageDataSource(private val context: Context) {
    
    suspend fun uploadImage(uri: Uri, path: String): Flow<Result<String>> = flow {
        try {
            // Create directory if needed
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), path)
            directory.mkdirs()
            
            // Generate filename
            val filename = "${System.currentTimeMillis()}.jpg"
            val file = File(directory, filename)
            
            // Copy image to local storage
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Return file path as "URL"
            val filePath = file.absolutePath
            emit(Result.Success(filePath))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val file = File(imageUrl)
            if (file.exists()) {
                file.delete()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### Testing

- ✅ Images save to device storage
- ✅ Images load correctly in UI
- ✅ Image paths stored in Firestore
- ✅ Images persist across app restarts
- ✅ Images deleted when post is deleted

### Notes

- This is a **development/testing strategy**
- For production, switch to Firebase Storage or another cloud service
- Local storage paths work with Coil/Picasso automatically
- No changes needed in UI code when switching to Firebase Storage

