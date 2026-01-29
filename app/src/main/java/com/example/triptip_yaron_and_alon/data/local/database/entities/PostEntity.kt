package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userImageUrl: String?,
    val text: String,
    val imageUrl: String?,
    val createdAt: Long,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeXid: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

