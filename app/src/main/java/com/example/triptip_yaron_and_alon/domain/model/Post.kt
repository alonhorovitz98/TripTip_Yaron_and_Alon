package com.example.triptip_yaron_and_alon.domain.model

data class Post(
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
    val placeXid: String? = null, // OpenTripMap xid if post is about a specific place
    val priceLevel: Int? = null, // Google Places price_level 0-4 ($ to $$$$)
    // Social features (likedBy stored in Firestore for real counts)
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val likedByCurrentUser: Boolean = false,
    val commentCount: Int = 0
)


