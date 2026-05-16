package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.*
import com.example.triptip_yaron_and_alon.data.local.database.entities.CommentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for comments.
 * Provides methods for CRUD operations on comments with Flow support for reactive updates.
 */
@Dao
interface CommentDao {
    
    /**
     * Get all comments for a specific post, ordered by creation time (newest first).
     * @param postId The ID of the post
     * @return Flow of comments list that updates automatically
     */
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt DESC")
    fun getCommentsByPost(postId: String): Flow<List<CommentEntity>>
    
    /**
     * Get all replies to a specific comment.
     * @param parentCommentId The ID of the parent comment
     * @return Flow of reply comments
     */
    @Query("SELECT * FROM comments WHERE parentCommentId = :parentCommentId ORDER BY createdAt ASC")
    fun getRepliesByComment(parentCommentId: String): Flow<List<CommentEntity>>
    
    /**
     * Get a specific comment by ID.
     * @param commentId The comment ID
     * @return Flow of the comment, or null if not found
     */
    @Query("SELECT * FROM comments WHERE id = :commentId")
    fun getCommentById(commentId: String): Flow<CommentEntity?>

    /**
     * Get a specific comment by ID (one-shot).
     * @param commentId The comment ID
     * @return The comment, or null if not found
     */
    @Query("SELECT * FROM comments WHERE id = :commentId")
    suspend fun getCommentByIdSync(commentId: String): CommentEntity?
    
    /**
     * Get comment count for a post.
     * @param postId The ID of the post
     * @return Flow of comment count
     */
    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: String): Flow<Int>
    
    /**
     * Insert a new comment.
     * @param comment The comment to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)
    
    /**
     * Insert multiple comments.
     * @param comments List of comments to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    /**
     * Synchronize comments for a post: delete existing ones and insert new ones.
     * Used to keep local cache in sync with remote even when items are deleted.
     */
    @Transaction
    suspend fun syncComments(postId: String, comments: List<CommentEntity>) {
        deleteByPost(postId)
        insertAll(comments)
    }
    
    /**
     * Update an existing comment.
     * @param comment The comment to update
     */
    @Update
    suspend fun update(comment: CommentEntity)
    
    /**
     * Delete a comment.
     * @param comment The comment to delete
     */
    @Delete
    suspend fun delete(comment: CommentEntity)

    /**
     * Delete a comment by its ID.
     */
    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteById(commentId: String)
    
    /**
     * Delete all comments for a specific post.
     * @param postId The ID of the post
     */
    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteByPost(postId: String)
    
    /**
     * Increment like count locally.
     */
    @Query("UPDATE comments SET likes = likes + 1 WHERE id = :commentId")
    suspend fun incrementLikes(commentId: String)

    /**
     * Decrement like count locally.
     */
    @Query("UPDATE comments SET likes = CASE WHEN likes > 0 THEN likes - 1 ELSE 0 END WHERE id = :commentId")
    suspend fun decrementLikes(commentId: String)

    /**
     * Clear all old cached comments (older than threshold).
     * @param threshold Timestamp threshold
     */
    @Query("DELETE FROM comments WHERE cachedAt < :threshold")
    suspend fun clearOldCache(threshold: Long)
}
