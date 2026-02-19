package com.example.triptip_yaron_and_alon.domain.model

data class TripItem(
    val id: String,
    val dayId: String,
    val postId: String? = null, // Optional - for posts
    val placeId: String? = null, // Optional - for places (OpenTripMap xid)
    val order: Int,
    val notes: String? = null,
    val post: Post? = null, // Loaded separately when needed
    val place: PlaceInfo? = null // Loaded separately when needed
)

