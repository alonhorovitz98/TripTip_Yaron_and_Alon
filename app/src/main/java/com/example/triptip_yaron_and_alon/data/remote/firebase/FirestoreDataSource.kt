package com.example.triptip_yaron_and_alon.data.remote.firebase

import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
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
     * Get posts by a specific user ID.
     * Returns Flow<List<Post>> that emits updates when posts change.
     */
    fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_POSTS)
            .whereEqualTo("userId", userId)
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
    
    // ==================== TRIPS ====================
    
    /**
     * Get all trips for a specific user.
     * Returns Flow<List<Trip>> that emits updates when trips change.
     * Note: Trips are loaded without nested days/items for performance.
     * Use loadTripWithNestedData() to get full trip with days and items.
     */
    fun getTrips(userId: String): Flow<List<Trip>> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_TRIPS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val trips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toTripBasic()
                } ?: emptyList()
                
                trySend(trips)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    /**
     * Get a single trip by ID (without nested days/items).
     * Returns Flow<Trip?> that emits updates when the trip changes.
     * Use loadTripWithNestedData() to get full trip with days and items.
     */
    fun getTripById(tripId: String): Flow<Trip?> = callbackFlow {
        val listenerRegistration = firestore.collection(Constants.COLLECTION_TRIPS)
            .document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val trip = snapshot?.toTripBasic()
                trySend(trip)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
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
     * Returns Result<Trip> with the updated trip.
     */
    suspend fun updateTrip(trip: Trip): Result<Trip> {
        return try {
            firestore.collection(Constants.COLLECTION_TRIPS)
                .document(trip.id)
                .set(trip.toMap())
                .await()
            
            // Update trip days
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
     * Returns Result<Unit> indicating success or failure.
     */
    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            // Delete trip days subcollection first
            val daysSnapshot = firestore.collection(Constants.COLLECTION_TRIPS)
                .document(tripId)
                .collection("days")
                .get()
                .await()
            
            daysSnapshot.documents.forEach { dayDoc ->
                dayDoc.reference.delete().await()
            }
            
            // Delete the trip
            firestore.collection(Constants.COLLECTION_TRIPS)
                .document(tripId)
                .delete()
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Save a trip day as a subcollection under the trip.
     */
    private suspend fun saveTripDay(tripId: String, day: TripDay) {
        val dayId = day.id.ifEmpty { UUID.randomUUID().toString() }
        val dayWithId = day.copy(id = dayId, tripId = tripId)
        
        firestore.collection(Constants.COLLECTION_TRIPS)
            .document(tripId)
            .collection("days")
            .document(dayId)
            .set(dayWithId.toMap())
            .await()
        
        // Save trip items as subcollection under the day
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
            TripItem(
                id = id,
                dayId = dayId,
                postId = getString("postId") ?: return null,
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
                placeXid = getString("placeXid")
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
            "placeXid" to placeXid
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
            "createdAt" to createdAt
        )
    }
    
    /**
     * Convert TripDay domain model to Firestore map.
     */
    private fun TripDay.toMap(): Map<String, Any?> {
        return mapOf(
            "tripId" to tripId,
            "dayNumber" to dayNumber,
            "date" to date
        )
    }
    
    /**
     * Convert TripItem domain model to Firestore map.
     */
    private fun TripItem.toMap(): Map<String, Any?> {
        return mapOf(
            "dayId" to dayId,
            "postId" to postId,
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

