package com.example.triptip_yaron_and_alon.domain.model

data class PlaceInfo(
    val xid: String, // OpenTripMap ID
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val categories: List<String>,
    val distance: Double? = null // Distance from post location in meters
)

