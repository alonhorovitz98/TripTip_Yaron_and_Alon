package com.example.triptip_yaron_and_alon.data.remote.firebase

import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firestore data source for notifications (likes, comments on my posts).
 */
class NotificationsDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = firestore.collection(COLLECTION_NOTIFICATIONS)

    suspend fun createNotification(
        recipientUserId: String,
        type: String,
        actorUserId: String,
        actorUserName: String,
        targetPostId: String?,
        message: String
    ): Result<Unit> {
        if (recipientUserId == actorUserId) return Result.Success(Unit) // Don't notify self
        return try {
            val id = UUID.randomUUID().toString()
            val data = hashMapOf(
                "userId" to recipientUserId,
                "type" to type,
                "actorUserId" to actorUserId,
                "actorUserName" to actorUserName,
                "targetPostId" to targetPostId,
                "message" to message,
                "isRead" to false,
                "createdAt" to System.currentTimeMillis()
            )
            collection.document(id).set(data).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    fun getNotificationsForUser(userId: String): Flow<List<NotificationDoc>> = callbackFlow {
        // No orderBy here — combining whereEqualTo + orderBy on different fields requires a
        // Firestore composite index. We sort client-side instead to avoid the crash.
        val listener = collection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                        close()
                    } else {
                        trySend(emptyList())
                    }
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        NotificationDoc(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            type = doc.getString("type") ?: "",
                            actorUserId = doc.getString("actorUserId") ?: "",
                            actorUserName = doc.getString("actorUserName") ?: "",
                            targetPostId = doc.getString("targetPostId"),
                            message = doc.getString("message") ?: "",
                            isRead = doc.getBoolean("isRead") ?: false,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                // Sort newest-first client-side
                trySend(list.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            collection.document(notificationId).update("isRead", true).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            // Single whereEqualTo — no composite index needed. Filter unread client-side.
            val docs = collection.whereEqualTo("userId", userId).get().await()
            val unreadDocs = docs.documents.filter { it.getBoolean("isRead") != true }
            if (unreadDocs.isNotEmpty()) {
                val batch = firestore.batch()
                unreadDocs.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit().await()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    data class NotificationDoc(
        val id: String,
        val userId: String,
        val type: String,
        val actorUserId: String,
        val actorUserName: String,
        val targetPostId: String?,
        val message: String,
        val isRead: Boolean,
        val createdAt: Long
    )

    companion object {
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val TYPE_LIKE = "LIKE"
        const val TYPE_COMMENT = "COMMENT"
    }
}
