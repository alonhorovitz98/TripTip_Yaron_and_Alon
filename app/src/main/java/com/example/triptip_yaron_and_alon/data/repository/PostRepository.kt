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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Repository for post operations.
 * Implements cache-first strategy: Room first, then Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PostRepository(
    private val postDao: PostDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: FirebaseStorageDataSource
) {
    
    /**
     * Feed: emit Room cache once, then stream Firestore snapshots continuously.
     * Room is updated on each snapshot but does **not** drive this Flow (avoids flatMapLatest
     * cancelling the listener whenever the local cache writes — that broke cross-device sync).
     */
    fun getPosts(): Flow<PostsFeedSnapshot> = channelFlow {
        var latest = withContext(Dispatchers.IO) {
            runCatching { PostMapper.toDomainList(postDao.getAllPosts().first()) }.getOrDefault(emptyList())
        }
        send(PostsFeedSnapshot(latest))
        try {
            firestoreDataSource.getPosts()
                .onEach { remotePosts ->
                    latest = remotePosts
                    withContext(Dispatchers.IO) {
                        runCatching { postDao.insertAll(PostMapper.toEntityList(remotePosts)) }
                    }
                }
                .collect { send(PostsFeedSnapshot(it)) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            send(PostsFeedSnapshot(latest, syncError = feedSyncErrorMessage(e)))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * My posts: same pattern as [getPosts] but filtered by [userId].
     */
    fun getMyPosts(userId: String): Flow<PostsFeedSnapshot> = channelFlow {
        var latest = withContext(Dispatchers.IO) {
            runCatching { PostMapper.toDomainList(postDao.getPostsByUser(userId).first()) }.getOrDefault(emptyList())
        }
        send(PostsFeedSnapshot(latest))
        try {
            firestoreDataSource.getPostsByUser(userId)
                .onEach { remote ->
                    latest = remote
                    withContext(Dispatchers.IO) {
                        runCatching { postDao.insertAll(PostMapper.toEntityList(remote)) }
                    }
                }
                .collect { send(PostsFeedSnapshot(it)) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            send(PostsFeedSnapshot(latest, syncError = feedSyncErrorMessage(e)))
        }
    }.flowOn(Dispatchers.IO)

    /** Wipe cached posts when switching accounts so the next user does not see stale data. */
    suspend fun clearLocalCache() {
        withContext(Dispatchers.IO) {
            postDao.deleteAll()
        }
    }

    private fun feedSyncErrorMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Can't load posts from the cloud. Make sure you're logged in and Firestore rules are deployed (see SETUP.md)."
            msg.contains("UNAUTHENTICATED", ignoreCase = true) ->
                "Sign in to see posts from everyone on TripTip."
            msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("network", ignoreCase = true) ->
                "No connection to TripTip cloud. Showing cached posts only."
            else -> "Can't sync posts from the cloud: ${e.message ?: "unknown error"}"
        }
    }

    /**
     * After profile edit, push new display name / photo into denormalized fields on posts and comments
     * so all devices see the same author line on existing content.
     */
    suspend fun propagateAuthorDisplayToPublishedContent(
        userId: String,
        userName: String,
        profileImageUrl: String?
    ) {
        firestoreDataSource.propagateUserProfileToPosts(userId, userName, profileImageUrl)
        firestoreDataSource.propagateUserProfileToComments(userId, userName, profileImageUrl)
    }

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
                .first() // Get first emission to update cache, don't collect forever
                .let { remotePosts ->
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
    fun getPostById(postId: String): Flow<Post?> = postDao.getPostById(postId)
        .map { entity -> entity?.let { PostMapper.toDomain(it) } }
        .catch { emit(null) }
        .flowOn(Dispatchers.IO)
        .flatMapLatest { cachedPost ->
            // Emit cached post immediately
            flow {
                emit(cachedPost)
                
                // Then fetch from Firestore and update cache
                try {
                    firestoreDataSource.getPostById(postId)
                        .catch { /* Ignore errors, keep using cache */ }
                        .collect { remotePost ->
                            remotePost?.let {
                                // Update cache in background
                                withContext(Dispatchers.IO) {
                                    postDao.insert(PostMapper.toEntity(it))
                                }
                                // Emit updated post from Firestore (already domain model)
                                emit(it)
                            } ?: run {
                                // If Firestore returns null, keep cached post
                                emit(cachedPost)
                            }
                        }
                } catch (e: Exception) {
                    // If Firestore fails, keep emitting cached data
                    emit(cachedPost)
                }
            }
        }
    
    
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
                val uploadResult = storageDataSource.uploadImageSync(imageUri, Constants.STORAGE_POST_IMAGES)
                
                when (uploadResult) {
                    is Result.Success -> {
                        imageUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        emit(uploadResult)
                        return@flow
                    }
                    is Result.Loading -> {
                        // Should not happen with sync function, but handle just in case
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
                
                val uploadResult = storageDataSource.uploadImageSync(imageUri, Constants.STORAGE_POST_IMAGES)
                
                when (uploadResult) {
                    is Result.Success -> {
                        imageUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        emit(uploadResult)
                        return@flow
                    }
                    is Result.Loading -> {
                        // Should not happen with sync function, but handle just in case
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
     * Like a post. Updates Firestore and local cache. Returns post owner id for notification.
     */
    suspend fun likePost(postId: String, userId: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            when (val result = firestoreDataSource.likePost(postId, userId)) {
                is Result.Success -> {
                    result.data?.let { ownerId ->
                        firestoreDataSource.getPostById(postId).first().let { updatedPost ->
                            updatedPost?.let { postDao.insert(PostMapper.toEntity(it)) }
                        }
                    }
                    Result.Success(result.data)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected"), "Loading")
            }
        }
    }
    
    /**
     * Unlike a post. Updates Firestore and local cache.
     */
    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            when (val result = firestoreDataSource.unlikePost(postId, userId)) {
                is Result.Success -> {
                    firestoreDataSource.getPostById(postId).first().let { updatedPost ->
                        updatedPost?.let { postDao.insert(PostMapper.toEntity(it)) }
                    }
                    Result.Success(Unit)
                }
                is Result.Error -> result
                is Result.Loading -> Result.Error(Exception("Unexpected"), "Loading")
            }
        }
    }
    
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

