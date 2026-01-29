package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.BuildConfig
import com.example.triptip_yaron_and_alon.data.remote.api.ApiClient
import com.example.triptip_yaron_and_alon.data.remote.api.OpenTripMapApiService
import com.example.triptip_yaron_and_alon.data.remote.api.WeatherApiService
import com.example.triptip_yaron_and_alon.data.remote.api.mapper.ApiMapper
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.WeatherInfo
import com.example.triptip_yaron_and_alon.util.Constants
import kotlinx.coroutines.Dispatchers
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
    private val openTripMapApiService: OpenTripMapApiService = ApiClient.openTripMapApiService
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
}

