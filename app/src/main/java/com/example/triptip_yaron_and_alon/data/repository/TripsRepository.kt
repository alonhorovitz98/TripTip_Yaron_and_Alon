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
import kotlinx.coroutines.flow.map
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
        firestore.observeTrip(tripId).map { trip ->
            withContext(Dispatchers.IO) {
                trip?.let { hydrateTrip(it) }
            }
        }

    suspend fun getDayForEditor(tripId: String, dayId: String): TripDay? = withContext(Dispatchers.IO) {
        val day = firestore.loadDay(tripId, dayId) ?: return@withContext null
        hydrateDay(day)
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

    suspend fun getTrip(tripId: String): Trip? = withContext(Dispatchers.IO) {
        firestore.loadFullTrip(tripId)?.let { hydrateTrip(it) }
    }

    suspend fun syncDaysForDateRange(
        tripId: String,
        startDateMillis: Long,
        endDateMillis: Long
    ) = firestore.syncDaysForDateRange(tripId, startDateMillis, endDateMillis)

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

    private suspend fun hydrateTrip(trip: Trip): Trip =
        trip.copy(days = trip.days.map { hydrateDay(it) })

    private suspend fun hydrateDay(day: TripDay): TripDay {
        val items = day.items.map { item ->
            if (item.type == DayItemType.POST) {
                val post = runCatching { postRepository.getPostById(item.value).first() }.getOrNull()
                item.copy(post = post)
            } else {
                item
            }
        }
        return day.copy(items = items)
    }
}
