package com.example.triptip_yaron_and_alon.data.remote.firebase

import com.example.triptip_yaron_and_alon.domain.model.Comment
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase data source for comments.
 * Handles CRUD operations for comments in Firestore.
 */
class CommentsDataSource {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val commentsCollection = firestore.collection("comments")
    
    /**
     * Get all comments for a specific post.
     * Returns Flow that emits updates in real-time.
     */
    fun getCommentsByPost(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(Exception(error))
                    return@addSnapshotListener
                }
                
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Comment(
                            id = doc.id,
                            postId = doc.getString("postId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userAvatarUrl = doc.getString("userAvatarUrl"),
                            text = doc.getString("text") ?: "",
                            parentCommentId = doc.getString("parentCommentId"),
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(comments)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Add a new comment to a post.
     */
    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ): Result<Comment> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            val commentId = UUID.randomUUID().toString()
            val comment = Comment(
                id = commentId,
                postId = postId,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonymous",
                userAvatarUrl = currentUser.photoUrl?.toString(),
                text = text,
                parentCommentId = parentCommentId,
                likes = 0,
                createdAt = System.currentTimeMillis()
            )
            
            val commentData = hashMapOf(
                "postId" to comment.postId,
                "userId" to comment.userId,
                "userName" to comment.userName,
                "userAvatarUrl" to comment.userAvatarUrl,
                "text" to comment.text,
                "parentCommentId" to comment.parentCommentId,
                "likes" to comment.likes,
                "createdAt" to comment.createdAt
            )
            
            commentsCollection.document(commentId).set(commentData).await()
            
            // Update comment count on post
            val postsCollection = firestore.collection("posts")
            postsCollection.document(postId)
                .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(e, "Failed to add comment: ${e.message}")
        }
    }
    
    /**
     * Delete a comment.
     */
    suspend fun deleteComment(commentId: String, postId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            // Verify ownership
            val doc = commentsCollection.document(commentId).get().await()
            if (doc.getString("userId") != currentUser.uid) {
                return Result.Error(Exception("Not authorized"), "Not authorized to delete this comment")
            }
            
            commentsCollection.document(commentId).delete().await()
            
            // Update comment count on post
            val postsCollection = firestore.collection("posts")
            postsCollection.document(postId)
                .update("commentCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete comment: ${e.message}")
        }
    }
    
    /**
     * Like a comment.
     */
    suspend fun likeComment(commentId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            commentsCollection.document(commentId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to like comment: ${e.message}")
        }
    }
    
    /**
     * Unlike a comment.
     */
    suspend fun unlikeComment(commentId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.Error(Exception("User not authenticated"), "User not authenticated")
            
            commentsCollection.document(commentId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to unlike comment: ${e.message}")
        }
    }
}
