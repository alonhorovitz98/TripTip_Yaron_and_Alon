package com.example.triptip_yaron_and_alon.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class WeatherResponseDto(
    @SerializedName("main")
    val main: MainDto,
    @SerializedName("weather")
    val weather: List<WeatherDto>,
    @SerializedName("wind")
    val wind: WindDto
)

data class MainDto(
    @SerializedName("temp")
    val temp: Double,
    @SerializedName("humidity")
    val humidity: Int
)

data class WeatherDto(
    @SerializedName("description")
    val description: String,
    @SerializedName("icon")
    val icon: String
)

data class WindDto(
    @SerializedName("speed")
    val speed: Double
)

