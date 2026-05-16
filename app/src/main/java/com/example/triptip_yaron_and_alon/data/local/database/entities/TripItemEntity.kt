package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_items",
    foreignKeys = [
        ForeignKey(
            entity = TripDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dayId"]),
        Index(value = ["postId"]),
        Index(value = ["placeId"])
    ]
)
data class TripItemEntity(
    @PrimaryKey
    val id: String,
    val dayId: String,
    val postId: String? = null, // Optional - for posts
    val placeId: String? = null, // Optional - for places (OpenTripMap xid)
    val order: Int,
    val notes: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

