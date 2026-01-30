package com.example.triptip_yaron_and_alon.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user notification.
 * Supports different notification types: LIKE, COMMENT, FOLLOW, TRIP_UPDATE
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String, // User who receives the notification
    val type: String, // LIKE, COMMENT, FOLLOW, TRIP_UPDATE
    val actorUserId: String, // User who performed the action
    val actorUserName: String,
    val actorUserAvatar: String?,
    val targetPostId: String?, // Related post ID (for likes, comments)
    val targetTripId: String?, // Related trip ID (for trip updates)
    val message: String, // Display message, e.g., "John liked your post"
    val isRead: Boolean = false,
    val createdAt: Long
)
