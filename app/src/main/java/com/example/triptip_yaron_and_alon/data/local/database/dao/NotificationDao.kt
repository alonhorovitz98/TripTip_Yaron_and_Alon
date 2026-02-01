package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.*
import com.example.triptip_yaron_and_alon.data.local.database.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notifications.
 * Provides methods for managing user notifications with Flow support.
 */
@Dao
interface NotificationDao {
    
    /**
     * Get all notifications for a user, ordered by creation time (newest first).
     * @param userId The user's ID
     * @return Flow of notifications list
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsByUser(userId: String): Flow<List<NotificationEntity>>
    
    /**
     * Get unread notifications for a user.
     * @param userId The user's ID
     * @return Flow of unread notifications
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(userId: String): Flow<List<NotificationEntity>>
    
    /**
     * Get unread notification count.
     * @param userId The user's ID
     * @return Flow of unread count
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: String): Flow<Int>
    
    /**
     * Get a specific notification by ID.
     * @param notificationId The notification ID
     * @return Flow of the notification
     */
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    fun getNotificationById(notificationId: String): Flow<NotificationEntity?>
    
    /**
     * Insert a new notification.
     * @param notification The notification to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)
    
    /**
     * Insert multiple notifications.
     * @param notifications List of notifications to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)
    
    /**
     * Update a notification (e.g., mark as read).
     * @param notification The notification to update
     */
    @Update
    suspend fun update(notification: NotificationEntity)
    
    /**
     * Mark a notification as read.
     * @param notificationId The notification ID
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)
    
    /**
     * Mark all notifications as read for a user.
     * @param userId The user's ID
     */
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
    
    /**
     * Delete a notification.
     * @param notification The notification to delete
     */
    @Delete
    suspend fun delete(notification: NotificationEntity)
    
    /**
     * Delete all notifications for a user.
     * @param userId The user's ID
     */
    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
    
    /**
     * Delete old read notifications (older than threshold).
     * @param threshold Timestamp threshold
     */
    @Query("DELETE FROM notifications WHERE isRead = 1 AND createdAt < :threshold")
    suspend fun deleteOldReadNotifications(threshold: Long)
}
