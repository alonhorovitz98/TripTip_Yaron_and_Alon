package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.data.local.database.dao.CommentDao
import com.example.triptip_yaron_and_alon.data.remote.firebase.CommentsDataSource
import com.example.triptip_yaron_and_alon.domain.mapper.CommentMapper
import com.example.triptip_yaron_and_alon.domain.model.Comment
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Repository for comments with cache-first strategy.
 * Manages comments from both local Room database and Firebase Firestore.
 */
class CommentsRepository(
    private val commentsDataSource: CommentsDataSource,
    private val commentDao: CommentDao
) {
    
    /**
     * Get all comments for a post with cache-first strategy.
     * Emits cached comments immediately, then updates from Firestore.
     */
    fun getCommentsByPost(postId: String): Flow<List<Comment>> {
        // Start listening to Firestore
        return commentsDataSource.getCommentsByPost(postId)
            .onEach { comments ->
                // Cache comments in Room
                val entities = CommentMapper.toEntityList(comments)
                commentDao.insertAll(entities)
            }
            .map { comments ->
                // Return Firestore comments
                comments
            }
    }
    
    /**
     * Add a new comment.
     */
    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ): Flow<Result<Comment>> {
        return kotlinx.coroutines.flow.flow {
            emit(Result.Loading)
            
            val result = commentsDataSource.addComment(postId, text, parentCommentId)
            
            when (result) {
                is Result.Success -> {
                    // Cache in Room
                    val entity = CommentMapper.toEntity(result.data)
                    commentDao.insert(entity)
                    emit(Result.Success(result.data))
                }
                is Result.Error -> emit(result)
                is Result.Loading -> emit(result)
            }
        }
    }
    
    /**
     * Delete a comment.
     */
    suspend fun deleteComment(commentId: String, postId: String): Flow<Result<Unit>> {
        return kotlinx.coroutines.flow.flow {
            emit(Result.Loading)
            
            val result = commentsDataSource.deleteComment(commentId, postId)
            
            when (result) {
                is Result.Success -> {
                    // Remove from Room cache
                    commentDao.getCommentById(commentId).collect { comment ->
                        comment?.let {
                            commentDao.delete(it)
                        }
                    }
                    emit(Result.Success(Unit))
                }
                is Result.Error -> emit(result)
                is Result.Loading -> emit(result)
            }
        }
    }
    
    /**
     * Like a comment.
     */
    suspend fun likeComment(commentId: String): Flow<Result<Unit>> {
        return kotlinx.coroutines.flow.flow {
            emit(Result.Loading)
            emit(commentsDataSource.likeComment(commentId))
        }
    }
    
    /**
     * Unlike a comment.
     */
    suspend fun unlikeComment(commentId: String): Flow<Result<Unit>> {
        return kotlinx.coroutines.flow.flow {
            emit(Result.Loading)
            emit(commentsDataSource.unlikeComment(commentId))
        }
    }
}
