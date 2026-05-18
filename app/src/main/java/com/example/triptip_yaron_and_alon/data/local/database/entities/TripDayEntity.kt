package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "simple_days",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class TripDayEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val dayOrder: Int,
    val dateMillis: Long? = null
)
