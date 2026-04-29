package com.example.triptip_yaron_and_alon.data.remote.firebase

import com.example.triptip_yaron_and_alon.domain.model.DayItem
import com.example.triptip_yaron_and_alon.domain.model.DayItemType
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Data source for Firestore operations.
 * All methods are asynchronous and use coroutines.
 */
class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    // ==================== POSTS ====================
    
    /**
     * Get all posts from Firestore.
     * Returns Flow<List<Post>> that emits updates when posts change.
     */
    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Close gracefully on permission errors so the listener is removed
                    // immediately and the Firestore SDK stops its internal retry loop.
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                        close()
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toPost()
                } ?: emptyList()
                
                trySend(posts)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    /**
     * Get posts by a specific user from Firestore.
     * No orderBy to avoid requiring a composite index; sorted client-side.
     */
    fun getPostsByUser(userId: String): Flow<List<Post>> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_POSTS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val posts = (snapshot?.documents?.mapNotNull { it.toPost() } ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(posts)
            }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Get a single post by ID.
     * Returns Flow<Post?> that emits updates when the post changes.
     */
    fun getPostById(postId: String): Flow<Post?> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                        close()
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val post = snapshot?.toPost()
                trySend(post)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    
    /**
     * Create a new post in Firestore.
     * Returns Result<Post> with the created post (including generated ID).
     */
    suspend fun createPost(post: Post): Result<Post> {
        return try {
            val postId = post.id.ifEmpty { UUID.randomUUID().toString() }
            val postWithId = post.copy(id = postId)
            
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .set(postWithId.toMap())
                .await()
            
            Result.Success(postWithId)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Update an existing post in Firestore.
     * Returns Result<Post> with the updated post.
     */
    suspend fun updatePost(post: Post): Result<Post> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(post.id)
                .set(post.toMap())
                .await()
            
            Result.Success(post)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Delete a post from Firestore.
     * Returns Result<Unit> indicating success or failure.
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .delete()
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Like a post (add userId to likedBy and increment likes count).
     * Returns the post's userId (owner) for creating a notification.
     */
    suspend fun likePost(postId: String, userId: String): Result<String?> {
        return try {
            val docRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            val doc = docRef.get().await()
            val ownerId = doc.getString("userId")
            docRef.update(
                mapOf(
                    "likedBy" to FieldValue.arrayUnion(userId),
                    "likes" to FieldValue.increment(1)
                )
            ).await()
            Result.Success(ownerId)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Unlike a post (remove userId from likedBy and decrement likes count).
     */
    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS).document(postId)
                .update(
                    mapOf(
                        "likedBy" to FieldValue.arrayRemove(userId),
                        "likes" to FieldValue.increment(-1)
                    )
                )
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    // ==================== TRIPS (Firestore — source of truth) ====================

    private fun FirebaseFirestore.trips() =
        collection(Constants.COLLECTION_TRIPS)

    private suspend fun touchTrip(tripId: String) = withContext(Dispatchers.IO) {
        firestore.trips().document(tripId)
            .update("updatedAt", System.currentTimeMillis())
            .await()
    }

    /**
     * List trips (metadata + [firestoreDayCount] from parent doc; subcollections not loaded).
     */
    fun getTrips(userId: String): Flow<List<Trip>> = callbackFlow {
        val reg = firestore.trips()
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
                val list = (snapshot?.documents?.mapNotNull { doc ->
                    if (!doc.exists()) return@mapNotNull null
                    Trip(
                        id = doc.id,
                        userId = doc.getString("userId") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        startDateMillis = if (doc.contains("startDateMillis")) doc.getLong("startDateMillis") else null,
                        endDateMillis = if (doc.contains("endDateMillis")) doc.getLong("endDateMillis") else null,
                        days = emptyList(),
                        firestoreDayCount = (doc.getLong("dayCount") ?: 0L).toInt()
                    )
                } ?: emptyList()).sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Live updates when the trip document changes (and when [touchTrip] runs after any day/item change).
     */
    fun observeTrip(tripId: String): Flow<Trip?> = callbackFlow {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        val reg = firestore.trips().document(tripId)
            .addSnapshotListener { _, e ->
                if (e != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                scope.launch {
                    val t = loadFullTrip(tripId)
                    trySend(t)
                }
            }
        awaitClose {
            reg.remove()
            job.cancel("observeTrip closed")
        }
    }

    suspend fun loadFullTrip(tripId: String): Trip? = withContext(Dispatchers.IO) {
        val doc = firestore.trips().document(tripId).get().await()
        if (!doc.exists()) return@withContext null
        val userId = doc.getString("userId") ?: return@withContext null
        val name = doc.getString("name") ?: return@withContext null
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        val dayDocs = doc.reference.collection("days").get().await()
            .documents
            .sortedBy { it.getLong("dayOrder") ?: 0L }
        val days = dayDocs.map { d ->
            val dayId = d.id
            val itemsSnap = d.reference.collection("items").get().await()
            val items = itemsSnap.documents
                .sortedBy { it.getLong("sortOrder") ?: 0L }
                .map { itemDoc ->
                    DayItem(
                        id = itemDoc.id,
                        dayId = dayId,
                        type = itemDoc.getString("type") ?: DayItemType.PLACE,
                        value = itemDoc.getString("value") ?: "",
                        sortOrder = (itemDoc.getLong("sortOrder") ?: 0L).toInt()
                    )
                }
            TripDay(
                id = dayId,
                tripId = tripId,
                dayOrder = (d.getLong("dayOrder") ?: 1L).toInt(),
                dateMillis = if (d.contains("dateMillis")) d.getLong("dateMillis") else null,
                items = items
            )
        }
        Trip(
            id = tripId,
            userId = userId,
            name = name,
            createdAt = createdAt,
            startDateMillis = if (doc.contains("startDateMillis")) doc.getLong("startDateMillis") else null,
            endDateMillis = if (doc.contains("endDateMillis")) doc.getLong("endDateMillis") else null,
            days = days,
            firestoreDayCount = days.size
        )
    }

    suspend fun loadDay(tripId: String, dayId: String): TripDay? = withContext(Dispatchers.IO) {
        val d = firestore.trips().document(tripId).collection("days").document(dayId).get().await()
        if (!d.exists()) return@withContext null
        if (d.getString("tripId") != tripId) return@withContext null
        val id = d.id
        val itemsSnap = d.reference.collection("items").get().await()
        val items = itemsSnap.documents
            .sortedBy { it.getLong("sortOrder") ?: 0L }
            .map { itemDoc ->
                DayItem(
                    id = itemDoc.id,
                    dayId = id,
                    type = itemDoc.getString("type") ?: DayItemType.PLACE,
                    value = itemDoc.getString("value") ?: "",
                    sortOrder = (itemDoc.getLong("sortOrder") ?: 0L).toInt()
                )
            }
        TripDay(
            id = id,
            tripId = tripId,
            dayOrder = (d.getLong("dayOrder") ?: 1L).toInt(),
            dateMillis = if (d.contains("dateMillis")) d.getLong("dateMillis") else null,
            items = items
        )
    }

    suspend fun createTrip(
        userId: String,
        name: String,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = hashMapOf<String, Any>(
            "userId" to userId,
            "name" to name.trim(),
            "createdAt" to now,
            "dayCount" to 0L,
            "updatedAt" to now
        )
        if (startDateMillis != null) data["startDateMillis"] = startDateMillis
        if (endDateMillis != null) data["endDateMillis"] = endDateMillis
        firestore.trips().document(id)
            .set(data)
            .await()
        id
    }

    suspend fun updateTrip(
        tripId: String,
        name: String,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ) = withContext(Dispatchers.IO) {
        val updates = hashMapOf<String, Any>(
            "name" to name.trim(),
            "updatedAt" to System.currentTimeMillis()
        )
        updates["startDateMillis"] = startDateMillis ?: FieldValue.delete()
        updates["endDateMillis"] = endDateMillis ?: FieldValue.delete()
        firestore.trips().document(tripId).update(updates).await()
    }

    suspend fun addDay(tripId: String, dateMillis: Long? = null): String = withContext(Dispatchers.IO) {
        val tripRef = firestore.trips().document(tripId)
        val daysCol = tripRef.collection("days")
        val all = daysCol.get().await()
        val next = (all.documents.map { it.getLong("dayOrder") ?: 0L }.maxOrNull() ?: 0L) + 1L
        val dayId = UUID.randomUUID().toString()
        val dayData = hashMapOf<String, Any>("tripId" to tripId, "dayOrder" to next)
        if (dateMillis != null) dayData["dateMillis"] = dateMillis
        val batch = firestore.batch()
        batch.set(daysCol.document(dayId), dayData)
        batch.update(
            tripRef,
            mapOf(
                "dayCount" to FieldValue.increment(1),
                "updatedAt" to System.currentTimeMillis()
            )
        )
        batch.commit().await()
        dayId
    }

    suspend fun setDayDate(tripId: String, dayId: String, dateMillis: Long?) = withContext(Dispatchers.IO) {
        val dayRef = firestore.trips().document(tripId).collection("days").document(dayId)
        if (dateMillis == null) {
            dayRef.update("dateMillis", FieldValue.delete()).await()
        } else {
            dayRef.update("dateMillis", dateMillis).await()
        }
        touchTrip(tripId)
    }

    suspend fun addDayItem(tripId: String, dayId: String, type: String, value: String) = withContext(Dispatchers.IO) {
        val items = firestore.trips().document(tripId).collection("days").document(dayId).collection("items")
        val list = items.get().await()
        val next = (list.documents.map { it.getLong("sortOrder") ?: 0L }.maxOrNull() ?: -1L) + 1L
        val id = UUID.randomUUID().toString()
        items.document(id)
            .set(
                mapOf(
                    "type" to type,
                    "value" to value,
                    "sortOrder" to next,
                    "dayId" to dayId
                )
            )
            .await()
        touchTrip(tripId)
    }

    suspend fun deleteDayItem(tripId: String, dayId: String, itemId: String) = withContext(Dispatchers.IO) {
        firestore.trips().document(tripId).collection("days").document(dayId).collection("items").document(itemId)
            .delete()
            .await()
        touchTrip(tripId)
    }

    suspend fun removeDay(tripId: String, dayId: String) = withContext(Dispatchers.IO) {
        val dayRef = firestore.trips().document(tripId).collection("days").document(dayId)
        val itemSnap = dayRef.collection("items").get().await()
        for (d in itemSnap.documents) {
            d.reference.delete().await()
        }
        dayRef.delete().await()
        firestore.trips().document(tripId)
            .update(
                mapOf(
                    "dayCount" to FieldValue.increment(-1),
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    suspend fun deleteTrip(tripId: String) = withContext(Dispatchers.IO) {
        val tripRef = firestore.trips().document(tripId)
        val days = tripRef.collection("days").get().await()
        for (day in days.documents) {
            val isnap = day.reference.collection("items").get().await()
            for (idoc in isnap.documents) {
                idoc.reference.delete().await()
            }
            day.reference.delete().await()
        }
        tripRef.delete().await()
    }
    
    // ==================== USERS ====================
    
    /**
     * Get user data from Firestore by user ID.
     * Returns Flow<User?> that emits updates when the user changes.
     */
    fun getUser(userId: String): Flow<User?> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                        close()
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toUser()
                trySend(user)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    /**
     * Create or update user data in Firestore.
     * Returns Result<User> with the saved user.
     */
    suspend fun saveUser(user: User): Result<User> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .set(user.toMap())
                .await()
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Update user profile in Firestore.
     * Returns Result<User> with the updated user.
     */
    suspend fun updateUser(user: User): Result<User> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .update(user.toMap())
                .await()
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    // ==================== MAPPING FUNCTIONS ====================
    
    /**
     * Convert Firestore document to Post domain model.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toPost(): Post? {
        return try {
            val likedByList = (get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            Post(
                id = id,
                userId = getString("userId") ?: return null,
                userName = getString("userName") ?: "",
                userImageUrl = getString("userImageUrl"),
                text = getString("text") ?: "",
                imageUrl = getString("imageUrl"),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                location = getString("location"),
                latitude = getDouble("latitude"),
                longitude = getDouble("longitude"),
                placeXid = getString("placeXid"),
                priceLevel = getLong("priceLevel")?.toInt()?.coerceIn(0, 4),
                likes = (getLong("likes") ?: 0).toInt(),
                likedBy = likedByList,
                likedByCurrentUser = false, // Set in UI from currentUserId
                commentCount = (getLong("commentCount") ?: 0).toInt()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert Post domain model to Firestore map.
     */
    private fun Post.toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "userImageUrl" to userImageUrl,
            "text" to text,
            "imageUrl" to imageUrl,
            "createdAt" to createdAt,
            "location" to location,
            "latitude" to latitude,
            "longitude" to longitude,
            "placeXid" to placeXid,
            "priceLevel" to priceLevel,
            "likes" to likes,
            "likedBy" to likedBy,
            "commentCount" to commentCount
        )
    }
    
    /**
     * Convert Firestore document to User domain model.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(): User? {
        return try {
            User(
                id = id,
                email = getString("email") ?: "",
                name = getString("name") ?: "",
                profileImageUrl = getString("profileImageUrl")
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert User domain model to Firestore map.
     */
    private fun User.toMap(): Map<String, Any?> {
        return mapOf(
            "email" to email,
            "name" to name,
            "profileImageUrl" to profileImageUrl
        )
    }
}

