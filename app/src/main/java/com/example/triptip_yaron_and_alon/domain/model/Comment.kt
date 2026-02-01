package com.example.triptip_yaron_and_alon.domain.model

/**
 * Domain model for a comment on a post.
 * Supports threaded comments via parentCommentId.
 */
data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val text: String,
    val parentCommentId: String?, // null for top-level comments
    val likes: Int = 0,
    val createdAt: Long
)
