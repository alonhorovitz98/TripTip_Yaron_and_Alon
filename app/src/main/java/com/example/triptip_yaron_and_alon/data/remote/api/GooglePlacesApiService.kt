package com.example.triptip_yaron_and_alon.data.remote.api

import com.example.triptip_yaron_and_alon.data.remote.api.dto.GooglePlacesAutocompleteResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.GooglePlaceDetailsResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.GooglePlacesNearbySearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Places API Service for autocomplete and place details.
 * Provides Google Maps-level quality autocomplete with place recommendations.
 */
interface GooglePlacesApiService {
    
    /**
     * Autocomplete search - returns place suggestions as user types.
     * This is the main method for location autocomplete.
     */
    @GET("autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("types") types: String? = null, // null = all types (cities, establishments, addresses, etc.)
        @Query("language") language: String = "en",
        @Query("components") components: String? = null // e.g., "country:us"
    ): GooglePlacesAutocompleteResponseDto
    
    /**
     * Get place details by place_id.
     * Use this after user selects a suggestion to get full details including coordinates.
     */
    @GET("details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String,
        @Query("fields") fields: String = "place_id,name,formatted_address,geometry,address_components,types"
    ): GooglePlaceDetailsResponseDto
    
    /**
     * Nearby Search - returns places near a location.
     * Use this to find places within a specified radius of a location.
     */
    @GET("nearbysearch/json")
    suspend fun nearbySearch(
        @Query("location") location: String, // "latitude,longitude"
        @Query("radius") radius: Int, // in meters
        @Query("key") apiKey: String,
        @Query("type") type: String? = null, // e.g., "restaurant", "tourist_attraction", null = all types
        @Query("language") language: String = "en"
    ): GooglePlacesNearbySearchResponseDto
}
