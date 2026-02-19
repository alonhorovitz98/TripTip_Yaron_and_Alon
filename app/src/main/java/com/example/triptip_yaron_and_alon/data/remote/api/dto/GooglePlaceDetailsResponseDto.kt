package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for Google Places Details API response.
 */
data class GooglePlaceDetailsResponseDto(
    val result: PlaceDetailsResultDto?,
    val status: String
)

data class PlaceDetailsResultDto(
    @SerializedName("place_id")
    val placeId: String,
    
    val name: String,
    
    @SerializedName("formatted_address")
    val formattedAddress: String,
    
    val geometry: GoogleGeometryDto?,
    
    @SerializedName("address_components")
    val addressComponents: List<AddressComponentDto>? = null,
    
    val types: List<String>? = null
)

data class GoogleGeometryDto(
    val location: GoogleLocationDto?
)

data class GoogleLocationDto(
    val lat: Double,
    val lng: Double
)

data class AddressComponentDto(
    @SerializedName("long_name")
    val longName: String,
    
    @SerializedName("short_name")
    val shortName: String,
    
    val types: List<String>
)
