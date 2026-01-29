package com.example.triptip_yaron_and_alon.data.remote.api

import com.example.triptip_yaron_and_alon.data.remote.api.dto.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Weather API Service.
 * Documentation: https://open-meteo.com/en/docs
 * No API key required - free and open source.
 */
interface WeatherApiService {
    @GET("forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh"
    ): WeatherResponseDto
}

