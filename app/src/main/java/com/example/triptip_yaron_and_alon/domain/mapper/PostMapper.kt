package com.example.triptip_yaron_and_alon.domain.mapper

import com.example.triptip_yaron_and_alon.data.local.database.entities.PostEntity
import com.example.triptip_yaron_and_alon.domain.model.Post

object PostMapper {
    /**
     * Converts PostEntity (Room) to Post (Domain)
     */
    fun toDomain(entity: PostEntity): Post {
        val likedBy = entity.likedBy.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return Post(
            id = entity.id,
            userId = entity.userId,
            userName = entity.userName,
            userImageUrl = entity.userImageUrl,
            text = entity.text,
            imageUrl = entity.imageUrl,
            createdAt = entity.createdAt,
            location = entity.location,
            latitude = entity.latitude,
            longitude = entity.longitude,
            placeXid = entity.placeXid,
            priceLevel = entity.priceLevel,
            likes = entity.likes,
            likedBy = likedBy,
            likedByCurrentUser = entity.likedByCurrentUser,
            commentCount = entity.commentCount
        )
    }
    
    /**
     * Converts Post (Domain) to PostEntity (Room)
     */
    fun toEntity(domain: Post): PostEntity {
        return PostEntity(
            id = domain.id,
            userId = domain.userId,
            userName = domain.userName,
            userImageUrl = domain.userImageUrl,
            text = domain.text,
            imageUrl = domain.imageUrl,
            createdAt = domain.createdAt,
            location = domain.location,
            latitude = domain.latitude,
            longitude = domain.longitude,
            placeXid = domain.placeXid,
            priceLevel = domain.priceLevel,
            likes = domain.likes,
            likedBy = domain.likedBy.joinToString(","),
            likedByCurrentUser = domain.likedByCurrentUser,
            commentCount = domain.commentCount,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Converts list of PostEntity to list of Post
     */
    fun toDomainList(entities: List<PostEntity>): List<Post> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Converts list of Post to list of PostEntity
     */
    fun toEntityList(domains: List<Post>): List<PostEntity> {
        return domains.map { toEntity(it) }
    }
}

