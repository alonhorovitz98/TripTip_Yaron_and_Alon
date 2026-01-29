package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo weather forecast response DTO.
 * Documentation: https://open-meteo.com/en/docs
 */
data class WeatherResponseDto(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("current")
    val current: CurrentWeatherDto,
    @SerializedName("current_units")
    val currentUnits: CurrentUnitsDto? = null
)

data class CurrentWeatherDto(
    @SerializedName("time")
    val time: String,
    @SerializedName("temperature_2m")
    val temperature2m: Double,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: Int,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: Double
)

data class CurrentUnitsDto(
    @SerializedName("temperature_2m")
    val temperature2m: String? = null,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: String? = null,
    @SerializedName("weather_code")
    val weatherCode: String? = null,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: String? = null
)

