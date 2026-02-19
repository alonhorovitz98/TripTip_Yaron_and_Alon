package com.example.triptip_yaron_and_alon.domain.mapper

import com.example.triptip_yaron_and_alon.data.local.database.entities.TripItemEntity
import com.example.triptip_yaron_and_alon.domain.model.TripItem

object TripItemMapper {
    /**
     * Converts TripItemEntity (Room) to TripItem (Domain)
     * Note: post and place objects are not included in entity, must be loaded separately if needed
     */
    fun toDomain(
        entity: TripItemEntity, 
        post: com.example.triptip_yaron_and_alon.domain.model.Post? = null,
        place: com.example.triptip_yaron_and_alon.domain.model.PlaceInfo? = null
    ): TripItem {
        return TripItem(
            id = entity.id,
            dayId = entity.dayId,
            postId = entity.postId,
            placeId = entity.placeId,
            order = entity.order,
            notes = entity.notes,
            post = post,
            place = place
        )
    }
    
    /**
     * Converts TripItem (Domain) to TripItemEntity (Room)
     * Note: post and place objects are ignored, only postId/placeId are stored
     */
    fun toEntity(domain: TripItem): TripItemEntity {
        return TripItemEntity(
            id = domain.id,
            dayId = domain.dayId,
            postId = domain.postId,
            placeId = domain.placeId,
            order = domain.order,
            notes = domain.notes,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Converts list of TripItemEntity to list of TripItem
     */
    fun toDomainList(
        entities: List<TripItemEntity>, 
        postsMap: Map<String, com.example.triptip_yaron_and_alon.domain.model.Post> = emptyMap(),
        placesMap: Map<String, com.example.triptip_yaron_and_alon.domain.model.PlaceInfo> = emptyMap()
    ): List<TripItem> {
        return entities.map { entity ->
            val post = entity.postId?.let { postsMap[it] }
            val place = entity.placeId?.let { placesMap[it] }
            toDomain(entity, post, place)
        }
    }
    
    /**
     * Converts list of TripItem to list of TripItemEntity
     */
    fun toEntityList(domains: List<TripItem>): List<TripItemEntity> {
        return domains.map { toEntity(it) }
    }
}

