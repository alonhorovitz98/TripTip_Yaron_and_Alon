package com.example.triptip_yaron_and_alon.domain.model

/**
 * Location suggestion for autocomplete.
 * Contains display name and coordinates.
 */
data class LocationSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: Long? = null,
    val city: String? = null,
    val country: String? = null
)
