package com.example.triptip_yaron_and_alon.domain.model

data class Trip(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val days: List<TripDay> = emptyList()
)

