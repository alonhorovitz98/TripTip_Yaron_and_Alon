package com.example.triptip_yaron_and_alon.domain.mapper

import com.example.triptip_yaron_and_alon.data.local.database.entities.TripEntity
import com.example.triptip_yaron_and_alon.domain.model.Trip

object TripMapper {
    /**
     * Converts TripEntity (Room) to Trip (Domain)
     * Note: days list is not included in entity, must be loaded separately
     */
    fun toDomain(entity: TripEntity, days: List<com.example.triptip_yaron_and_alon.domain.model.TripDay> = emptyList()): Trip {
        return Trip(
            id = entity.id,
            userId = entity.userId,
            title = entity.title,
            description = entity.description,
            createdAt = entity.createdAt,
            days = days
        )
    }
    
    /**
     * Converts Trip (Domain) to TripEntity (Room)
     * Note: days list is ignored, stored separately in TripDayEntity
     */
    fun toEntity(domain: Trip): TripEntity {
        return TripEntity(
            id = domain.id,
            userId = domain.userId,
            title = domain.title,
            description = domain.description,
            createdAt = domain.createdAt,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Converts list of TripEntity to list of Trip
     */
    fun toDomainList(entities: List<TripEntity>, daysMap: Map<String, List<com.example.triptip_yaron_and_alon.domain.model.TripDay>> = emptyMap()): List<Trip> {
        return entities.map { entity ->
            toDomain(entity, daysMap[entity.id] ?: emptyList())
        }
    }
    
    /**
     * Converts list of Trip to list of TripEntity
     */
    fun toEntityList(domains: List<Trip>): List<TripEntity> {
        return domains.map { toEntity(it) }
    }
}

