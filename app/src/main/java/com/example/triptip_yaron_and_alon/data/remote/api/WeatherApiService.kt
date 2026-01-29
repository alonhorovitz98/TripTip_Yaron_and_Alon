package com.example.triptip_yaron_and_alon.data.remote.api

import com.example.triptip_yaron_and_alon.data.remote.api.dto.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // Use metric for Celsius
    ): WeatherResponseDto
}

