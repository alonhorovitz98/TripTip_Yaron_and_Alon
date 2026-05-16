package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for Google Places Autocomplete API response.
 */
data class GooglePlacesAutocompleteResponseDto(
    val predictions: List<PredictionDto>,
    val status: String
)

data class PredictionDto(
    @SerializedName("description")
    val description: String,
    
    @SerializedName("place_id")
    val placeId: String,
    
    @SerializedName("structured_formatting")
    val structuredFormatting: StructuredFormattingDto?,
    
    val types: List<String>? = null
)

data class StructuredFormattingDto(
    @SerializedName("main_text")
    val mainText: String,
    
    @SerializedName("secondary_text")
    val secondaryText: String
)
