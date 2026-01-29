package com.example.triptip_yaron_and_alon.domain.mapper

import com.example.triptip_yaron_and_alon.data.local.database.entities.TripDayEntity
import com.example.triptip_yaron_and_alon.domain.model.TripDay

object TripDayMapper {
    /**
     * Converts TripDayEntity (Room) to TripDay (Domain)
     * Note: items list is not included in entity, must be loaded separately
     */
    fun toDomain(entity: TripDayEntity, items: List<com.example.triptip_yaron_and_alon.domain.model.TripItem> = emptyList()): TripDay {
        return TripDay(
            id = entity.id,
            tripId = entity.tripId,
            dayNumber = entity.dayNumber,
            date = entity.date,
            items = items
        )
    }
    
    /**
     * Converts TripDay (Domain) to TripDayEntity (Room)
     * Note: items list is ignored, stored separately in TripItemEntity
     */
    fun toEntity(domain: TripDay): TripDayEntity {
        return TripDayEntity(
            id = domain.id,
            tripId = domain.tripId,
            dayNumber = domain.dayNumber,
            date = domain.date,
            cachedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Converts list of TripDayEntity to list of TripDay
     */
    fun toDomainList(entities: List<TripDayEntity>, itemsMap: Map<String, List<com.example.triptip_yaron_and_alon.domain.model.TripItem>> = emptyMap()): List<TripDay> {
        return entities.map { entity ->
            toDomain(entity, itemsMap[entity.id] ?: emptyList())
        }
    }
    
    /**
     * Converts list of TripDay to list of TripDayEntity
     */
    fun toEntityList(domains: List<TripDay>): List<TripDayEntity> {
        return domains.map { toEntity(it) }
    }
}

