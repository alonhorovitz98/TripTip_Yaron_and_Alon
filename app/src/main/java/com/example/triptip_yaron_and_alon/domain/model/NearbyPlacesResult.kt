package com.example.triptip_yaron_and_alon.domain.model

/**
 * Result of a nearby places search with pagination support.
 */
data class NearbyPlacesResult(
    val places: List<PlaceInfo>,
    val nextPageToken: String? = null // Token for loading next page
)
