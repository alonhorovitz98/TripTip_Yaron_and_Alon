package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.BuildConfig
import com.example.triptip_yaron_and_alon.data.remote.api.ApiClient
import com.example.triptip_yaron_and_alon.data.remote.api.GeocodingApiService
import com.example.triptip_yaron_and_alon.data.remote.api.GooglePlacesApiService
import com.example.triptip_yaron_and_alon.data.remote.api.OpenTripMapApiService
import com.example.triptip_yaron_and_alon.data.remote.api.WeatherApiService
import com.example.triptip_yaron_and_alon.data.remote.api.mapper.ApiMapper
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.WeatherInfo
import com.example.triptip_yaron_and_alon.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for external API operations (Weather and Places).
 * All methods are asynchronous and use Flow.
 */
class PlaceInfoRepository(
    private val weatherApiService: WeatherApiService = ApiClient.weatherApiService,
    private val openTripMapApiService: OpenTripMapApiService = ApiClient.openTripMapApiService,
    private val geocodingApiService: GeocodingApiService = ApiClient.geocodingApiService,
    private val googlePlacesApiService: GooglePlacesApiService = ApiClient.googlePlacesApiService
) {
    
    /**
     * Get weather information for given coordinates.
     * Returns Flow<WeatherInfo> that emits weather data or completes with error.
     * Uses Open-Meteo API (no API key required - free and open source).
     */
    fun getWeather(latitude: Double, longitude: Double): Flow<WeatherInfo> = flow {
        val response = weatherApiService.getCurrentWeather(
            latitude = latitude,
            longitude = longitude
        )
        
        val weatherInfo = ApiMapper.toWeatherInfo(response)
        emit(weatherInfo)
    }.catch { e ->
        // Re-throw with more context
        throw Exception("Failed to fetch weather: ${e.message}", e)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get nearby places for given coordinates.
     * Returns Flow<List<PlaceInfo>> that emits list of places or completes with error.
     */
    fun getNearbyPlaces(
        latitude: Double,
        longitude: Double,
        radius: Int = Constants.NEARBY_PLACES_RADIUS
    ): Flow<List<PlaceInfo>> = flow {
        val apiKey = getOpenTripMapApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenTripMap API key is not configured")
        }
        
        val response = openTripMapApiService.getNearbyPlaces(
            radius = radius,
            longitude = longitude,
            latitude = latitude,
            apiKey = apiKey,
            limit = 20
        )
        
        val places = ApiMapper.toPlaceInfoList(response, latitude, longitude)
        emit(places)
    }.catch { e ->
        // Re-throw with more context
        throw Exception("Failed to fetch nearby places: ${e.message}", e)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get detailed information for a specific place by xid.
     * Returns Flow<PlaceInfo> that emits place details or completes with error.
     */
    fun getPlaceDetails(xid: String): Flow<PlaceInfo> = flow {
        val apiKey = getOpenTripMapApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("OpenTripMap API key is not configured")
        }
        
        val response = openTripMapApiService.getPlaceDetails(
            xid = xid,
            apiKey = apiKey
        )
        
        val placeInfo = ApiMapper.toPlaceInfo(response)
        emit(placeInfo)
    }.catch { e ->
        // Re-throw with more context
        throw Exception("Failed to fetch place details: ${e.message}", e)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search for location suggestions (autocomplete).
     * Returns Flow<List<LocationSuggestion>> that emits location suggestions.
     * Uses Google Places API first (better quality, faster), falls back to Nominatim if needed.
     */
    fun searchLocationSuggestions(query: String): Flow<List<LocationSuggestion>> = flow {
        if (query.length < 2) {
            emit(emptyList())
            return@flow
        }
        
        try {
            // Try Google Places first (better quality, no rate limit delay)
            val apiKey = getGooglePlacesApiKey()
            if (apiKey.isNotBlank()) {
                val response = googlePlacesApiService.autocomplete(
                    input = query,
                    apiKey = apiKey,
                    types = null, // null = all types (cities, establishments, addresses, etc.) for better recommendations
                    language = "en"
                )
                
                if (response.status == "OK" && response.predictions.isNotEmpty()) {
                    val suggestions = response.predictions.map { 
                        ApiMapper.toLocationSuggestionFromGoogle(it) 
                    }
                    emit(suggestions)
                    return@flow
                }
            }
        } catch (e: Exception) {
            // Fall back to Nominatim if Google Places fails
        }
        
        // Fallback to Nominatim (slower, but free)
        // Respect Nominatim rate limit (1 request per second)
        delay(1100) // Slightly more than 1 second to be safe
        
        val response = geocodingApiService.searchLocations(
            query = query,
            limit = 10
        )
        
        val suggestions = ApiMapper.toLocationSuggestionList(response)
        emit(suggestions)
    }.catch { e ->
        // Re-throw with more context
        throw Exception("Failed to search locations: ${e.message}", e)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get place details from Google Places API by place_id.
     * Use this when user selects a suggestion to get coordinates and full address.
     */
    fun getGooglePlaceDetails(placeId: String): Flow<LocationSuggestion> = flow {
        val apiKey = getGooglePlacesApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Google Places API key is not configured")
        }
        
        val response = googlePlacesApiService.getPlaceDetails(
            placeId = placeId,
            apiKey = apiKey
        )
        
        if (response.status == "OK" && response.result != null) {
            val suggestion = ApiMapper.toLocationSuggestionFromPlaceDetails(response.result)
            emit(suggestion)
        } else {
            throw Exception("Place not found: ${response.status}")
        }
    }.catch { e ->
        throw Exception("Failed to get place details: ${e.message}", e)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Geocode a location name to get coordinates.
     * Returns Flow<Pair<Double, Double>?> that emits (latitude, longitude) or null if not found.
     * Uses OpenStreetMap Nominatim API (free, no API key required).
     */
    fun geocodeLocation(locationName: String): Flow<Pair<Double, Double>?> = flow {
        if (locationName.isBlank()) {
            emit(null)
            return@flow
        }
        
        // Respect Nominatim rate limit (1 request per second)
        delay(1100)
        
        val response = geocodingApiService.geocodeLocation(
            locationName = locationName,
            limit = 1
        )
        
        if (response.isEmpty()) {
            emit(null)
        } else {
            val result = response.first()
            val lat = result.latitude.toDoubleOrNull()
            val lon = result.longitude.toDoubleOrNull()
            
            if (lat != null && lon != null) {
                emit(Pair(lat, lon))
            } else {
                emit(null)
            }
        }
    }.catch { e ->
        // Re-throw with more context
        throw Exception("Failed to geocode location: ${e.message}", e)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get OpenTripMap API key from BuildConfig or Constants.
     * Priority: BuildConfig > Constants
     * Note: Open-Meteo doesn't require an API key (free and open source)
     */
    private fun getOpenTripMapApiKey(): String {
        return try {
            // Try BuildConfig first (if configured)
            if (BuildConfig.OPENTRIPMAP_API_KEY.isNotBlank()) {
                BuildConfig.OPENTRIPMAP_API_KEY
            } else {
                // Fallback to Constants (may be empty)
                Constants.OPENTRIPMAP_API_KEY
            }
        } catch (e: Exception) {
            // BuildConfig field might not exist yet, use Constants
            Constants.OPENTRIPMAP_API_KEY
        }
    }
    
    /**
     * Get Google Places API key from BuildConfig or Constants.
     * Priority: BuildConfig > Constants
     */
    private fun getGooglePlacesApiKey(): String {
        return try {
            // Try BuildConfig first (if configured)
            if (BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank()) {
                BuildConfig.GOOGLE_PLACES_API_KEY
            } else {
                // Fallback to Constants (may be empty)
                Constants.GOOGLE_PLACES_API_KEY
            }
        } catch (e: Exception) {
            // BuildConfig field might not exist yet, use Constants
            Constants.GOOGLE_PLACES_API_KEY
        }
    }
}

