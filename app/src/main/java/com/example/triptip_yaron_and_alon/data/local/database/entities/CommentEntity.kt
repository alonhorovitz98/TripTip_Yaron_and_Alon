package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a comment on a post.
 * Supports threaded comments via parentCommentId.
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val text: String,
    val imageUrl: String? = null,
    val parentCommentId: String?, // null for top-level comments, ID for replies
    val likes: Int = 0,
    val createdAt: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
