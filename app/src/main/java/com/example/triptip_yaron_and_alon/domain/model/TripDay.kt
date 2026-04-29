package com.example.triptip_yaron_and_alon.domain.model

data class TripDay(
    val id: String,
    val tripId: String,
    val dayOrder: Int,
    val dateMillis: Long? = null,
    val items: List<DayItem> = emptyList()
)
