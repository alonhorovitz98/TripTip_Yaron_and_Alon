package com.example.triptip_yaron_and_alon.data.remote.api.mapper

import com.example.triptip_yaron_and_alon.data.remote.api.dto.GeocodingResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.GooglePlacesAutocompleteResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.GooglePlaceDetailsResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.NearbyPlacesResponseDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsDto
import com.example.triptip_yaron_and_alon.data.remote.api.dto.WeatherResponseDto
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.WeatherInfo

/**
 * Mapper functions to convert API DTOs to domain models.
 */
object ApiMapper {
    
    /**
     * Convert WeatherResponseDto (Open-Meteo) to WeatherInfo domain model.
     * Weather codes from Open-Meteo use WMO weather interpretation codes.
     */
    fun toWeatherInfo(dto: WeatherResponseDto): WeatherInfo {
        val current = dto.current
        return WeatherInfo(
            temperature = current.temperature2m,
            description = getWeatherDescription(current.weatherCode),
            icon = getWeatherIcon(current.weatherCode),
            humidity = current.relativeHumidity2m,
            windSpeed = current.windSpeed10m
        )
    }
    
    /**
     * Convert WMO weather code to human-readable description.
     * Based on WMO Weather interpretation codes (WW).
     */
    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow fall"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
    
    /**
     * Convert WMO weather code to icon identifier.
     * Returns a simple icon identifier that can be used with weather icon libraries.
     */
    private fun getWeatherIcon(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "01d" // Clear sky
            1, 2 -> "02d" // Partly cloudy
            3 -> "04d" // Overcast
            45, 48 -> "50d" // Fog
            51, 53, 55, 56, 57 -> "09d" // Drizzle
            61, 63, 65, 66, 67 -> "10d" // Rain
            71, 73, 75, 77 -> "13d" // Snow
            80, 81, 82 -> "09d" // Rain showers
            85, 86 -> "13d" // Snow showers
            95, 96, 99 -> "11d" // Thunderstorm
            else -> "01d" // Default
        }
    }
    
    /**
     * Convert NearbyPlacesResponseDto to List<PlaceInfo>.
     */
    fun toPlaceInfoList(dto: NearbyPlacesResponseDto, referenceLat: Double, referenceLon: Double): List<PlaceInfo> {
        return dto.features.mapNotNull { feature ->
            try {
                val coordinates: List<Double> = feature.geometry.coordinates
                if (coordinates.size < 2) return@mapNotNull null
                
                val longitude: Double = coordinates[0]
                val latitude: Double = coordinates[1]
                
                PlaceInfo(
                    xid = feature.properties.xid,
                    name = feature.properties.name,
                    description = null, // Will be loaded from place details if needed
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = null, // Will be loaded from place details if needed
                    categories = feature.properties.kinds.split(",").map { it.trim() },
                    distance = feature.properties.distance
                )
            } catch (e: Exception) {
                null // Skip invalid features
            }
        }
    }
    
    /**
     * Convert PlaceDetailsDto to PlaceInfo domain model.
     */
    fun toPlaceInfo(dto: PlaceDetailsDto): PlaceInfo {
        return PlaceInfo(
            xid = dto.xid,
            name = dto.name,
            description = dto.wikipediaExtracts?.text,
            latitude = dto.point.latitude,
            longitude = dto.point.longitude,
            imageUrl = dto.preview?.source,
            categories = dto.kinds.split(",").map { it.trim() },
            distance = null
        )
    }
    
    /**
     * Update PlaceInfo with details from PlaceDetailsDto.
     */
    fun updatePlaceInfoWithDetails(placeInfo: PlaceInfo, details: PlaceDetailsDto): PlaceInfo {
        return placeInfo.copy(
            description = details.wikipediaExtracts?.text ?: placeInfo.description,
            imageUrl = details.preview?.source ?: placeInfo.imageUrl
        )
    }
    
    /**
     * Convert GeocodingResponseDto to LocationSuggestion domain model.
     */
    fun toLocationSuggestion(dto: GeocodingResponseDto): LocationSuggestion {
        return LocationSuggestion(
            displayName = dto.displayName,
            latitude = dto.latitude.toDoubleOrNull() ?: 0.0,
            longitude = dto.longitude.toDoubleOrNull() ?: 0.0,
            placeId = dto.placeId,
            city = dto.address?.city ?: dto.address?.town ?: dto.address?.village,
            country = dto.address?.country
        )
    }
    
    /**
     * Convert list of GeocodingResponseDto to List<LocationSuggestion>.
     */
    fun toLocationSuggestionList(dtos: List<GeocodingResponseDto>): List<LocationSuggestion> {
        return dtos.map { toLocationSuggestion(it) }
    }
    
    /**
     * Convert Google Places Autocomplete Prediction to LocationSuggestion.
     * Note: Coordinates will be 0.0 initially, should be filled from place details.
     */
    fun toLocationSuggestionFromGoogle(prediction: com.example.triptip_yaron_and_alon.data.remote.api.dto.PredictionDto): LocationSuggestion {
        return LocationSuggestion(
            displayName = prediction.description,
            latitude = 0.0, // Will be filled from place details when selected
            longitude = 0.0, // Will be filled from place details when selected
            placeId = null, // Nominatim place_id not applicable
            googlePlaceId = prediction.placeId, // Store Google Places place_id
            city = prediction.structuredFormatting?.secondaryText,
            country = null // Extract from address components if needed
        )
    }
    
    /**
     * Convert Google Place Details to LocationSuggestion with coordinates.
     */
    fun toLocationSuggestionFromPlaceDetails(result: com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto): LocationSuggestion {
        val location = result.geometry?.location
        val city = result.addressComponents?.find { 
            it.types.contains("locality") || it.types.contains("administrative_area_level_1")
        }?.longName
        
        val country = result.addressComponents?.find { 
            it.types.contains("country")
        }?.longName
        
        return LocationSuggestion(
            displayName = result.formattedAddress,
            latitude = location?.lat ?: 0.0,
            longitude = location?.lng ?: 0.0,
            placeId = null, // Nominatim place_id not applicable
            googlePlaceId = result.placeId, // Store Google Places place_id
            city = city,
            country = country
        )
    }
}

