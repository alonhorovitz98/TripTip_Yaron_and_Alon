package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.domain.model.DayItem
import com.example.triptip_yaron_and_alon.domain.model.DayItemType
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Trips: Firestore is the source of truth. Post previews for day editor use [PostRepository] cache/remote.
 */
class TripsRepository(
    private val firestore: FirestoreDataSource,
    private val postRepository: PostRepository
) {

    fun observeTripsForUser(userId: String): Flow<List<Trip>> =
        firestore.getTrips(userId)

    fun observeTrip(tripId: String): Flow<Trip?> =
        firestore.observeTrip(tripId)

    suspend fun getDayForEditor(tripId: String, dayId: String): TripDay? = withContext(Dispatchers.IO) {
        val day = firestore.loadDay(tripId, dayId) ?: return@withContext null
        val items: List<DayItem> = day.items.map { e ->
            if (e.type == DayItemType.POST) {
                val post = runCatching { postRepository.getPostById(e.value).first() }.getOrNull()
                DayItem(e.id, e.dayId, e.type, e.value, e.sortOrder, post)
            } else e
        }
        TripDay(
            id = day.id,
            tripId = day.tripId,
            dayOrder = day.dayOrder,
            dateMillis = day.dateMillis,
            items = items
        )
    }

    suspend fun createTrip(
        userId: String,
        name: String,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): String = firestore.createTrip(userId, name, startDateMillis, endDateMillis)

    suspend fun updateTrip(
        tripId: String,
        name: String,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ) = firestore.updateTrip(tripId, name, startDateMillis, endDateMillis)

    suspend fun addDay(tripId: String, dateMillis: Long? = null): String =
        firestore.addDay(tripId, dateMillis)

    suspend fun setDayDate(tripId: String, dayId: String, dateMillis: Long?) =
        firestore.setDayDate(tripId, dayId, dateMillis)

    suspend fun addDayItem(tripId: String, dayId: String, type: String, value: String) =
        firestore.addDayItem(tripId, dayId, type, value)

    suspend fun deleteDayItem(tripId: String, dayId: String, itemId: String) =
        firestore.deleteDayItem(tripId, dayId, itemId)

    suspend fun removeDay(tripId: String, dayId: String) =
        firestore.removeDay(tripId, dayId)

    suspend fun deleteTrip(tripId: String) =
        firestore.deleteTrip(tripId)
}
