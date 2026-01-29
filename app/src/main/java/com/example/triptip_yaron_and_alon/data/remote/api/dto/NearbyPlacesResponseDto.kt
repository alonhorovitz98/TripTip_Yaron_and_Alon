package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class NearbyPlacesResponseDto(
    @SerializedName("features")
    val features: List<FeatureDto>
)

data class FeatureDto(
    @SerializedName("properties")
    val properties: PlacePropertiesDto,
    @SerializedName("geometry")
    val geometry: GeometryDto
)

data class PlacePropertiesDto(
    @SerializedName("xid")
    val xid: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("kinds")
    val kinds: String, // Comma-separated categories
    @SerializedName("dist")
    val distance: Double? = null
)

data class GeometryDto(
    @SerializedName("coordinates")
    val coordinates: List<Double> // [longitude, latitude]
)

