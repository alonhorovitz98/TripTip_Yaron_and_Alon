package com.example.triptip_yaron_and_alon.data.repository

import android.net.Uri
import com.example.triptip_yaron_and_alon.data.local.database.dao.PostDao
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.domain.mapper.PostMapper
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for post operations.
 * Implements cache-first strategy: Room first, then Firestore.
 */
class PostRepository(
    private val postDao: PostDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: FirebaseStorageDataSource
) {
    
    /**
     * Get all posts with cache-first strategy.
     * Emits cached posts immediately, then fetches from Firestore and updates cache.
     */
    fun getPosts(): Flow<List<Post>> = flow {
        // Emit cached posts immediately
        postDao.getAllPosts()
            .map { entities -> PostMapper.toDomainList(entities) }
            .catch { emit(emptyList()) }
            .collect { cachedPosts ->
                emit(cachedPosts)
            }
        
        // Fetch from Firestore in background and update cache
        firestoreDataSource.getPosts()
            .catch { /* Ignore errors, use cache */ }
            .collect { remotePosts ->
                withContext(Dispatchers.IO) {
                    postDao.insertAll(PostMapper.toEntityList(remotePosts))
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get posts with pagination (lazy loading).
     * Returns posts from cache first, then fetches from Firestore.
     */
    fun getPostsPaginated(page: Int, pageSize: Int = Constants.DEFAULT_PAGINATION_SIZE): Flow<List<Post>> = flow {
        val offset = page * pageSize
        
        // Get cached posts for this page
        val cachedPosts = withContext(Dispatchers.IO) {
            postDao.getPostsPaginated(pageSize, offset)
        }
        emit(PostMapper.toDomainList(cachedPosts))
        
        // Fetch from Firestore and update cache (in background)
        // Note: This will update cache for future requests, but won't re-emit for this call
        try {
            firestoreDataSource.getPosts()
                .catch { /* Ignore errors, use cache */ }
                .collect { remotePosts ->
                    withContext(Dispatchers.IO) {
                        postDao.insertAll(PostMapper.toEntityList(remotePosts))
                    }
                }
        } catch (e: Exception) {
            // Ignore errors, use cached data
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get a single post by ID with cache-first strategy.
     */
    fun getPostById(postId: String): Flow<Post?> = flow {
        // Emit cached post immediately
        postDao.getPostById(postId)
            .map { entity -> entity?.let { PostMapper.toDomain(it) } }
            .catch { emit(null) }
            .collect { cachedPost ->
                emit(cachedPost)
            }
        
        // Fetch from Firestore and update cache
        firestoreDataSource.getPostById(postId)
            .catch { /* Ignore errors, use cache */ }
            .collect { remotePost ->
                remotePost?.let {
                    withContext(Dispatchers.IO) {
                        postDao.insert(PostMapper.toEntity(it))
                    }
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get posts by a specific user ID.
     */
    fun getUserPosts(userId: String): Flow<List<Post>> = flow {
        // Emit cached posts immediately
        postDao.getPostsByUser(userId)
            .map { entities -> PostMapper.toDomainList(entities) }
            .catch { emit(emptyList()) }
            .collect { cachedPosts ->
                emit(cachedPosts)
            }
        
        // Fetch from Firestore and update cache
        firestoreDataSource.getUserPosts(userId)
            .catch { /* Ignore errors, use cache */ }
            .collect { remotePosts ->
                withContext(Dispatchers.IO) {
                    postDao.insertAll(PostMapper.toEntityList(remotePosts))
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create a new post.
     * Uploads image, saves to Firestore, and caches in Room.
     */
    fun createPost(post: Post, imageUri: Uri?): Flow<Result<Post>> = flow {
        emit(Result.Loading)
        
        try {
            var imageUrl: String? = null
            
            // Upload image if provided
            if (imageUri != null) {
                val uploadResult = storageDataSource.uploadImage(imageUri, Constants.STORAGE_POST_IMAGES)
                    .first() // Get first emission instead of collecting
                
                when (uploadResult) {
                    is Result.Success -> {
                        imageUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        emit(uploadResult)
                        return@flow
                    }
                    is Result.Loading -> {
                        // Should not happen with first(), but handle just in case
                        emit(Result.Error(Exception("Unexpected loading state"), "Upload failed"))
                        return@flow
                    }
                }
            }
            
            // Create post with image URL
            val postWithImage = post.copy(imageUrl = imageUrl)
            
            // Save to Firestore
            val firestoreResult = firestoreDataSource.createPost(postWithImage)
            when (firestoreResult) {
                is Result.Success -> {
                    val createdPost = firestoreResult.data
                    // Cache in Room
                    withContext(Dispatchers.IO) {
                        postDao.insert(PostMapper.toEntity(createdPost))
                    }
                    emit(Result.Success(createdPost))
                }
                is Result.Error -> emit(firestoreResult)
                is Result.Loading -> emit(firestoreResult)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update an existing post.
     * Updates Firestore and Room cache.
     */
    fun updatePost(post: Post, imageUri: Uri?): Flow<Result<Post>> = flow {
        emit(Result.Loading)
        
        try {
            var imageUrl: String? = post.imageUrl
            
            // Upload new image if provided
            if (imageUri != null) {
                // Delete old image if exists
                post.imageUrl?.let { oldImageUrl ->
                    storageDataSource.deleteImage(oldImageUrl)
                }
                
                val uploadResult = storageDataSource.uploadImage(imageUri, Constants.STORAGE_POST_IMAGES)
                    .first() // Get first emission instead of collecting
                
                when (uploadResult) {
                    is Result.Success -> {
                        imageUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        emit(uploadResult)
                        return@flow
                    }
                    is Result.Loading -> {
                        // Should not happen with first(), but handle just in case
                        emit(Result.Error(Exception("Unexpected loading state"), "Upload failed"))
                        return@flow
                    }
                }
            }
            
            // Update post with new image URL
            val updatedPost = post.copy(imageUrl = imageUrl)
            
            // Update in Firestore
            val firestoreResult = firestoreDataSource.updatePost(updatedPost)
            when (firestoreResult) {
                is Result.Success -> {
                    // Update Room cache
                    withContext(Dispatchers.IO) {
                        postDao.insert(PostMapper.toEntity(updatedPost))
                    }
                    emit(Result.Success(updatedPost))
                }
                is Result.Error -> emit(firestoreResult)
                is Result.Loading -> emit(firestoreResult)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a post.
     * Deletes from Firestore, Room cache, and storage.
     */
    fun deletePost(postId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        
        try {
            // Get post to delete image
            val post = withContext(Dispatchers.IO) {
                postDao.getPostById(postId)
                    .catch { emit(null) }
                    .firstOrNull()
                    ?.let { PostMapper.toDomain(it) }
            }
            
            // Delete from Firestore
            val firestoreResult = firestoreDataSource.deletePost(postId)
            when (firestoreResult) {
                is Result.Success -> {
                    // Delete image from storage
                    post?.imageUrl?.let { imageUrl ->
                        storageDataSource.deleteImage(imageUrl)
                    }
                    
                    // Delete from Room cache
                    withContext(Dispatchers.IO) {
                        postDao.deleteById(postId)
                    }
                    
                    emit(Result.Success(Unit))
                }
                is Result.Error -> emit(firestoreResult)
                is Result.Loading -> emit(firestoreResult)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
}

