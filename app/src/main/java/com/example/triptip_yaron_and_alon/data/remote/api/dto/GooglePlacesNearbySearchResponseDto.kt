package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for Google Places Nearby Search API response.
 */
data class GooglePlacesNearbySearchResponseDto(
    val results: List<NearbyPlaceDto>,
    val status: String,
    @SerializedName("next_page_token")
    val nextPageToken: String? = null
)

data class NearbyPlaceDto(
    @SerializedName("place_id")
    val placeId: String,
    
    val name: String,
    
    @SerializedName("vicinity")
    val vicinity: String? = null, // Short address
    
    @SerializedName("formatted_address")
    val formattedAddress: String? = null, // Full address
    
    val geometry: GoogleNearbyGeometryDto? = null,
    
    @SerializedName("photos")
    val photos: List<PhotoDto>? = null,
    
    val types: List<String>? = null,
    
    val rating: Double? = null,
    
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int? = null
)

data class GoogleNearbyGeometryDto(
    val location: GoogleNearbyLocationDto
)

data class GoogleNearbyLocationDto(
    val lat: Double,
    val lng: Double
)

data class PhotoDto(
    @SerializedName("photo_reference")
    val photoReference: String,
    
    @SerializedName("width")
    val width: Int? = null,
    
    @SerializedName("height")
    val height: Int? = null
)
