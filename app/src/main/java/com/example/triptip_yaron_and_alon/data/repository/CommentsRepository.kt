package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.data.local.database.dao.CommentDao
import com.example.triptip_yaron_and_alon.data.remote.firebase.CommentsDataSource
import com.example.triptip_yaron_and_alon.domain.mapper.CommentMapper
import com.example.triptip_yaron_and_alon.domain.model.Comment
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Repository for comments with cache-first strategy.
 * Manages comments from both local Room database and Firebase Firestore.
 */
class CommentsRepository(
    private val commentsDataSource: CommentsDataSource,
    private val commentDao: CommentDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    
    /**
     * Get all comments for a post with cache-first strategy.
     * Returns a Flow that emits from Room immediately, while launching a background 
     * sync from Firestore to keep the local cache updated.
     */
    fun getCommentsByPost(postId: String): Flow<List<Comment>> {
        // Start background sync
        externalScope.launch {
            commentsDataSource.getCommentsByPost(postId).collect { remoteComments ->
                // Sync local DB with remote (handles additions, updates, and deletions)
                commentDao.syncComments(postId, CommentMapper.toEntityList(remoteComments))
            }
        }
        
        // Return reactive flow from Room
        return commentDao.getCommentsByPost(postId)
            .map { CommentMapper.toDomainList(it) }
            .distinctUntilChanged()
    }
    
    /**
     * Add a new comment.
     */
    suspend fun addComment(
        postId: String,
        text: String,
        imageUrl: String? = null,
        parentCommentId: String? = null,
        userName: String? = null,
        userAvatarUrl: String? = null
    ): Flow<Result<Comment>> = flow {
        emit(Result.Loading)
        
        val result = commentsDataSource.addComment(postId, text, imageUrl, parentCommentId, userName, userAvatarUrl)
        
        if (result is Result.Success) {
            // Cache in Room immediately
            commentDao.insert(CommentMapper.toEntity(result.data))
        }
        emit(result)
    }
    
    /**
     * Delete a comment.
     */
    suspend fun deleteComment(commentId: String, postId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        
        val result = commentsDataSource.deleteComment(commentId, postId)
        
        if (result is Result.Success) {
            // Remove from Room cache
            commentDao.deleteById(commentId)
        }
        emit(result)
    }
    
    /**
     * Like a comment with optimistic local update.
     */
    suspend fun likeComment(commentId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        
        // Optimistic update
        commentDao.incrementLikes(commentId)
        
        val result = commentsDataSource.likeComment(commentId)
        if (result is Result.Error) {
            // Rollback if remote fails
            commentDao.decrementLikes(commentId)
        }
        emit(result)
    }
    
    /**
     * Unlike a comment with optimistic local update.
     */
    suspend fun unlikeComment(commentId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        
        // Optimistic update
        commentDao.decrementLikes(commentId)
        
        val result = commentsDataSource.unlikeComment(commentId)
        if (result is Result.Error) {
            // Rollback if remote fails
            commentDao.incrementLikes(commentId)
        }
        emit(result)
    }
}
