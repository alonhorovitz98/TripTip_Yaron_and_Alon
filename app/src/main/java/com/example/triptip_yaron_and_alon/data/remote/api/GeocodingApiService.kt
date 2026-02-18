package com.example.triptip_yaron_and_alon.data.remote.api

import com.example.triptip_yaron_and_alon.data.remote.api.dto.GeocodingResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenStreetMap Nominatim Geocoding API Service.
 * Free, no API key required. Rate limit: 1 request per second.
 * Documentation: https://nominatim.org/release-docs/develop/api/Search/
 */
interface GeocodingApiService {
    
    /**
     * Search for locations by name (autocomplete/suggestions).
     * Returns list of location suggestions.
     */
    @GET("search")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<GeocodingResponseDto>
    
    /**
     * Geocode a location name to get coordinates.
     * Returns single best match or list.
     */
    @GET("search")
    suspend fun geocodeLocation(
        @Query("q") locationName: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<GeocodingResponseDto>
}
