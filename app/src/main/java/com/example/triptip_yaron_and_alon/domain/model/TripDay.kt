package com.example.triptip_yaron_and_alon.domain.model

data class TripDay(
    val id: String,
    val tripId: String,
    val dayNumber: Int,
    val date: Long? = null,
    val items: List<TripItem> = emptyList()
)

