package com.example.triptip_yaron_and_alon.domain.mapper

import com.example.triptip_yaron_and_alon.data.local.database.entities.UserEntity
import com.example.triptip_yaron_and_alon.domain.model.User

object UserMapper {
    /**
     * Converts UserEntity (Room) to User (Domain)
     */
    fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            email = entity.email,
            name = entity.name,
            profileImageUrl = entity.profileImageUrl
        )
    }
    
    /**
     * Converts User (Domain) to UserEntity (Room)
     */
    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.id,
            email = domain.email,
            name = domain.name,
            profileImageUrl = domain.profileImageUrl,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Converts list of UserEntity to list of User
     */
    fun toDomainList(entities: List<UserEntity>): List<User> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Converts list of User to list of UserEntity
     */
    fun toEntityList(domains: List<User>): List<UserEntity> {
        return domains.map { toEntity(it) }
    }
}

