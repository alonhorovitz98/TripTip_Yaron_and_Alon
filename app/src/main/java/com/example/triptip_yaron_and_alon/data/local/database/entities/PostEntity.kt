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
    val priceLevel: Int? = null,
    // Social features (likedBy stored as comma-separated ids in Room)
    val likes: Int = 0,
    val likedBy: String = "",
    val likedByCurrentUser: Boolean = false,
    val commentCount: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
)


