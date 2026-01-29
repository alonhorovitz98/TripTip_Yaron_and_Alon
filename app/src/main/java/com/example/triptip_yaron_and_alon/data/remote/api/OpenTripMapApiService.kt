package com.example.triptip_yaron_and_alon.data.remote.api

import com.example.triptip_yaron_and_alon.data.remote.api.dto.NearbyPlacesResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenTripMapApiService {
    @GET("places/radius")
    suspend fun getNearbyPlaces(
        @Query("radius") radius: Int, // in meters
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("apikey") apiKey: String,
        @Query("limit") limit: Int = 20
    ): NearbyPlacesResponseDto
    
    @GET("places/xid/{xid}")
    suspend fun getPlaceDetails(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String
    ): PlaceDetailsDto
}

