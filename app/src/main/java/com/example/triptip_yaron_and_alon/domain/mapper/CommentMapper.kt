package com.example.triptip_yaron_and_alon.domain.mapper

import com.example.triptip_yaron_and_alon.data.local.database.entities.CommentEntity
import com.example.triptip_yaron_and_alon.domain.model.Comment

/**
 * Mapper to convert between Comment domain model and CommentEntity.
 */
object CommentMapper {
    
    /**
     * Convert CommentEntity to Comment domain model.
     */
    fun toDomain(entity: CommentEntity): Comment {
        return Comment(
            id = entity.id,
            postId = entity.postId,
            userId = entity.userId,
            userName = entity.userName,
            userAvatarUrl = entity.userAvatarUrl,
            text = entity.text,
            imageUrl = entity.imageUrl,
            parentCommentId = entity.parentCommentId,
            likes = entity.likes,
            createdAt = entity.createdAt
        )
    }
    
    /**
     * Convert Comment domain model to CommentEntity.
     */
    fun toEntity(comment: Comment): CommentEntity {
        return CommentEntity(
            id = comment.id,
            postId = comment.postId,
            userId = comment.userId,
            userName = comment.userName,
            userAvatarUrl = comment.userAvatarUrl,
            text = comment.text,
            imageUrl = comment.imageUrl,
            parentCommentId = comment.parentCommentId,
            likes = comment.likes,
            createdAt = comment.createdAt,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Convert list of entities to domain models.
     */
    fun toDomainList(entities: List<CommentEntity>): List<Comment> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to entities.
     */
    fun toEntityList(comments: List<Comment>): List<CommentEntity> {
        return comments.map { toEntity(it) }
    }
}
