package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

