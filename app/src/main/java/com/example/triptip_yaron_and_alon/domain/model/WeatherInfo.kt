package com.example.triptip_yaron_and_alon.domain.model

data class WeatherInfo(
    val temperature: Double, // in Celsius
    val description: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double
)

