package com.example.triptip_yaron_and_alon.domain.model

data class PostWithPlaceInfo(
    val post: Post,
    val weather: WeatherInfo?,
    val nearbyPlaces: List<PlaceInfo>
)

