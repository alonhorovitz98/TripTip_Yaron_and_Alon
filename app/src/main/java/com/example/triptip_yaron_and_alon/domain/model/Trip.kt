package com.example.triptip_yaron_and_alon.domain.model

/**
 * Trip: [name] at trip level; [days] loaded in detail. [firestoreDayCount] is the denormalized
 * count on the `trips/{id}` document for list screens when [days] is not loaded yet.
 */
data class Trip(
    val id: String,
    val userId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val days: List<TripDay> = emptyList(),
    val firestoreDayCount: Int = 0
)
