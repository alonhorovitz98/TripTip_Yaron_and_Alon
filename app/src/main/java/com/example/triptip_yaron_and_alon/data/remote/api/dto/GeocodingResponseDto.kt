package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for OpenStreetMap Nominatim geocoding API response.
 * Free, no API key required.
 */
data class GeocodingResponseDto(
    @SerializedName("display_name")
    val displayName: String,
    
    @SerializedName("lat")
    val latitude: String,
    
    @SerializedName("lon")
    val longitude: String,
    
    @SerializedName("place_id")
    val placeId: Long,
    
    val type: String? = null,
    
    val importance: Double? = null,
    
    val address: AddressDto? = null
)

data class AddressDto(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val state: String? = null,
    val country: String? = null,
    val country_code: String? = null
)
