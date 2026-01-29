package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class PlaceDetailsDto(
    @SerializedName("xid")
    val xid: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("wikipedia_extracts")
    val wikipediaExtracts: WikipediaExtractsDto?,
    @SerializedName("point")
    val point: PointDto,
    @SerializedName("preview")
    val preview: PreviewDto?,
    @SerializedName("kinds")
    val kinds: String
)

data class WikipediaExtractsDto(
    @SerializedName("text")
    val text: String?
)

data class PointDto(
    @SerializedName("lon")
    val longitude: Double,
    @SerializedName("lat")
    val latitude: Double
)

data class PreviewDto(
    @SerializedName("source")
    val source: String?
)

