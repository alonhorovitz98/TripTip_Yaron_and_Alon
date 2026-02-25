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
    val formattedAddress: String? = null,
    
    val geometry: GoogleGeometryDto?,
    
    @SerializedName("address_components")
    val addressComponents: List<AddressComponentDto>? = null,
    
    val types: List<String>? = null,
    
    // Additional details
    @SerializedName("formatted_phone_number")
    val formattedPhoneNumber: String? = null,
    
    @SerializedName("international_phone_number")
    val internationalPhoneNumber: String? = null,
    
    val website: String? = null,
    
    val rating: Double? = null,
    
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int? = null,
    
    @SerializedName("opening_hours")
    val openingHours: OpeningHoursDto? = null,
    
    val photos: List<PlacePhotoDto>? = null,
    
    val reviews: List<ReviewDto>? = null,
    
    @SerializedName("editorial_summary")
    val editorialSummary: EditorialSummaryDto? = null,
    
    val vicinity: String? = null
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

data class OpeningHoursDto(
    @SerializedName("open_now")
    val openNow: Boolean? = null,
    
    @SerializedName("weekday_text")
    val weekdayText: List<String>? = null
)

data class PlacePhotoDto(
    @SerializedName("photo_reference")
    val photoReference: String,
    
    val width: Int? = null,
    
    val height: Int? = null
)

data class ReviewDto(
    @SerializedName("author_name")
    val authorName: String,
    
    @SerializedName("author_url")
    val authorUrl: String? = null,
    
    val language: String? = null,
    
    @SerializedName("profile_photo_url")
    val profilePhotoUrl: String? = null,
    
    val rating: Int,
    
    @SerializedName("relative_time_description")
    val relativeTimeDescription: String,
    
    val text: String? = null,
    
    val time: Long? = null
)

data class EditorialSummaryDto(
    val overview: String? = null
)
