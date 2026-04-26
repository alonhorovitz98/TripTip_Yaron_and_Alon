package com.example.triptip_yaron_and_alon.data.remote.firebase

import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
                    close(error)
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
                    close(error)
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
    
    // ==================== TRIPS ====================
    
    /**
     * Get all trips for a specific user.
     * Returns Flow<List<Trip>> that emits updates when trips change.
     * Note: Trips are loaded without nested days/items for performance.
     * Use loadTripWithNestedData() to get full trip with days and items.
     */
    fun getTrips(userId: String): Flow<List<Trip>> = callbackFlow {
        // No orderBy — whereEqualTo + orderBy on different fields requires a composite index.
        // Sort client-side instead to keep the query simple and index-free.
        val listenerRegistration = firestore.collection(Constants.COLLECTION_TRIPS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val trips = (snapshot?.documents?.mapNotNull { doc -> doc.toTripBasic() }
                    ?: emptyList()).sortedByDescending { it.createdAt }
                trySend(trips)
            }
        awaitClose { listenerRegistration.remove() }
    }
    
    
    /**
     * Load a trip with all nested days and items.
     * This is a suspend function that loads the complete trip structure.
     */
    suspend fun loadTripWithNestedData(tripId: String): Result<Trip> {
        return try {
            val tripDoc = firestore.collection(Constants.COLLECTION_TRIPS)
                .document(tripId)
                .get()
                .await()
            
            val trip = tripDoc.toTrip() ?: return Result.Error(
                IllegalStateException("Trip not found"),
                "Trip with ID $tripId not found"
            )
            
            Result.Success(trip)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Create a new trip in Firestore.
     * Returns Result<Trip> with the created trip (including generated ID).
     */
    suspend fun createTrip(trip: Trip): Result<Trip> {
        return try {
            val tripId = trip.id.ifEmpty { UUID.randomUUID().toString() }
            val tripWithId = trip.copy(id = tripId)
            
            firestore.collection(Constants.COLLECTION_TRIPS)
                .document(tripId)
                .set(tripWithId.toMap())
                .await()
            
            // Save trip days as subcollection
            tripWithId.days.forEach { day ->
                saveTripDay(tripId, day)
            }
            
            Result.Success(tripWithId)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Update an existing trip in Firestore.
     * Removes day/item documents that are no longer present in [trip], writes current ones.
     * Returns Result<Trip> with the updated trip.
     */
    suspend fun updateTrip(trip: Trip): Result<Trip> {
        return try {
            val tripRef = firestore.collection(Constants.COLLECTION_TRIPS).document(trip.id)

            tripRef.set(trip.toMap()).await()

            // Determine which day IDs should remain
            val currentDayIds = trip.days.map { it.id }.toSet()

            // Delete days (and their items) that were removed from the trip
            val existingDaysSnapshot = tripRef.collection("days").get().await()
            existingDaysSnapshot.documents.forEach { dayDoc ->
                if (dayDoc.id !in currentDayIds) {
                    val itemsSnapshot = dayDoc.reference.collection("items").get().await()
                    itemsSnapshot.documents.forEach { it.reference.delete().await() }
                    dayDoc.reference.delete().await()
                }
            }

            // Write / update remaining days (saveTripDay handles items internally)
            trip.days.forEach { day ->
                saveTripDay(trip.id, day)
            }

            Result.Success(trip)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Delete a trip from Firestore.
     * Recursively deletes days and their items subcollections, then the trip itself.
     * Returns Result<Unit> indicating success or failure.
     */
    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            val tripRef = firestore.collection(Constants.COLLECTION_TRIPS).document(tripId)

            // Delete items subcollection under each day, then the day itself
            val daysSnapshot = tripRef.collection("days").get().await()
            daysSnapshot.documents.forEach { dayDoc ->
                val itemsSnapshot = dayDoc.reference.collection("items").get().await()
                itemsSnapshot.documents.forEach { itemDoc ->
                    itemDoc.reference.delete().await()
                }
                dayDoc.reference.delete().await()
            }

            // Delete the trip document
            tripRef.delete().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Save a trip day as a subcollection under the trip.
     * Removes item documents that are no longer present in [day].
     */
    private suspend fun saveTripDay(tripId: String, day: TripDay) {
        val dayId = day.id.ifEmpty { UUID.randomUUID().toString() }
        val dayWithId = day.copy(id = dayId, tripId = tripId)

        val dayRef = firestore.collection(Constants.COLLECTION_TRIPS)
            .document(tripId)
            .collection("days")
            .document(dayId)

        dayRef.set(dayWithId.toMap()).await()

        // Delete items that were removed from this day
        val currentItemIds = dayWithId.items.map { it.id }.toSet()
        val existingItemsSnapshot = dayRef.collection("items").get().await()
        existingItemsSnapshot.documents.forEach { itemDoc ->
            if (itemDoc.id !in currentItemIds) {
                itemDoc.reference.delete().await()
            }
        }

        // Write / update remaining items
        dayWithId.items.forEach { item ->
            saveTripItem(tripId, dayId, item)
        }
    }
    
    /**
     * Save a trip item as a subcollection under the trip day.
     */
    private suspend fun saveTripItem(tripId: String, dayId: String, item: TripItem) {
        val itemId = item.id.ifEmpty { UUID.randomUUID().toString() }
        val itemWithId = item.copy(id = itemId, dayId = dayId)
        
        firestore.collection(Constants.COLLECTION_TRIPS)
            .document(tripId)
            .collection("days")
            .document(dayId)
            .collection("items")
            .document(itemId)
            .set(itemWithId.toMap())
            .await()
    }
    
    /**
     * Convert Firestore document to Trip (basic, without nested data).
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toTripBasic(): Trip? {
        return try {
            Trip(
                id = id,
                userId = getString("userId") ?: return null,
                title = getString("title") ?: return null,
                description = getString("description"),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                startDate = getLong("startDate"),
                endDate = getLong("endDate"),
                days = emptyList() // Nested data loaded separately
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Load trip with all nested days and items.
     */
    private suspend fun com.google.firebase.firestore.DocumentSnapshot.toTrip(): Trip? {
        return try {
            val tripId = id
            val userId = getString("userId") ?: return null
            val title = getString("title") ?: return null
            val description = getString("description")
            val createdAt = getLong("createdAt") ?: System.currentTimeMillis()
            val startDate = getLong("startDate")
            val endDate = getLong("endDate")
            
            // Load days
            val daysSnapshot = reference.collection("days").get().await()
            val days = daysSnapshot.documents.mapNotNull { dayDoc ->
                dayDoc.toTripDay(tripId)
            }
            
            Trip(
                id = tripId,
                userId = userId,
                title = title,
                description = description,
                createdAt = createdAt,
                startDate = startDate,
                endDate = endDate,
                days = days.sortedBy { it.dayNumber }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert a Firestore document to TripDay.
     */
    private suspend fun com.google.firebase.firestore.DocumentSnapshot.toTripDay(tripId: String): TripDay? {
        return try {
            val dayId = id
            val dayNumber = getLong("dayNumber")?.toInt() ?: return null
            val date = getLong("date")
            val description = getString("description")
            
            // Load items
            val itemsSnapshot = reference.collection("items").get().await()
            val items = itemsSnapshot.documents.mapNotNull { itemDoc ->
                itemDoc.toTripItem(dayId)
            }
            
            TripDay(
                id = dayId,
                tripId = tripId,
                dayNumber = dayNumber,
                date = date,
                description = description,
                items = items.sortedBy { it.order }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert a Firestore document to TripItem.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toTripItem(dayId: String): TripItem? {
        return try {
            val postId = getString("postId")
            val placeId = getString("placeId")
            if (postId == null && placeId == null) return null
            TripItem(
                id = id,
                dayId = dayId,
                postId = postId,
                placeId = placeId,
                order = getLong("order")?.toInt() ?: 0,
                notes = getString("notes")
            )
        } catch (e: Exception) {
            null
        }
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
                    close(error)
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
     * Convert Trip domain model to Firestore map.
     */
    private fun Trip.toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "createdAt" to createdAt,
            "startDate" to startDate,
            "endDate" to endDate
        )
    }
    
    /**
     * Convert TripDay domain model to Firestore map.
     */
    private fun TripDay.toMap(): Map<String, Any?> {
        return mapOf(
            "tripId" to tripId,
            "dayNumber" to dayNumber,
            "date" to date,
            "description" to description
        )
    }
    
    /**
     * Convert TripItem domain model to Firestore map.
     */
    private fun TripItem.toMap(): Map<String, Any?> {
        return mapOf(
            "dayId" to dayId,
            "postId" to postId,
            "placeId" to placeId,
            "order" to order,
            "notes" to notes
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

