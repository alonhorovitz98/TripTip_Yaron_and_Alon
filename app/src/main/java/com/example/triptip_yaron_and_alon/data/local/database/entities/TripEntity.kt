package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "simple_trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val createdAt: Long
)
