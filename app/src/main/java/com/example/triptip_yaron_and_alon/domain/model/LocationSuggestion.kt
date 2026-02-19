package com.example.triptip_yaron_and_alon.domain.model

/**
 * Location suggestion for autocomplete.
 * Contains display name and coordinates.
 * Supports both Nominatim (placeId as Long) and Google Places (googlePlaceId as String).
 */
data class LocationSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: Long? = null, // Nominatim place_id
    val googlePlaceId: String? = null, // Google Places place_id
    val city: String? = null,
    val country: String? = null
)
