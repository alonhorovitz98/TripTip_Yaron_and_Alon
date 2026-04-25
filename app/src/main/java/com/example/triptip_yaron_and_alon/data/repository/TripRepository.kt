package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.data.local.database.dao.TripDao
import com.example.triptip_yaron_and_alon.data.local.database.dao.TripDayDao
import com.example.triptip_yaron_and_alon.data.local.database.dao.TripItemDao
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.domain.mapper.TripDayMapper
import com.example.triptip_yaron_and_alon.domain.mapper.TripItemMapper
import com.example.triptip_yaron_and_alon.domain.mapper.TripMapper
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for trip operations.
 * Implements cache-first strategy: Room first, then Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripRepository(
    private val tripDao: TripDao,
    private val tripDayDao: TripDayDao,
    private val tripItemDao: TripItemDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    
    /**
     * Get all trips for a user with cache-first strategy.
     * Emits cached trips immediately, then fetches from Firestore and updates cache.
     */
    fun getTrips(userId: String): Flow<List<Trip>> = tripDao.getTripsByUser(userId)
        .map { entities ->
            entities.map { entity ->
                val days = loadDaysForTrip(entity.id)
                TripMapper.toDomain(entity, days)
            }
        }
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
        .flatMapLatest { cachedTrips ->
            flow {
                emit(cachedTrips)
                try {
                    firestoreDataSource.getTrips(userId)
                        .catch {
                            // Keep showing cached data on Firestore error.
                        }
                        .collect { remoteTrips ->
                            if (remoteTrips.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    // Only sync trip-level metadata (title, dates, etc.).
                                    // getTrips() returns trips with days = emptyList() for performance
                                    // (basic documents only). Calling saveTripsWithNestedData() here
                                    // would treat the empty days list as "all days deleted" and wipe
                                    // every day row from Room. Use saveTripMetadataOnly() instead.
                                    remoteTrips.forEach { trip ->
                                        tripDao.insert(TripMapper.toEntity(trip))
                                    }
                                }
                            }
                        }
                } catch (_: Exception) {
                    // Firestore error already handled by the catch operator above.
                }
            }
        }
    
    /**
     * Get a single trip by ID with cache-first strategy.
     * Emits cached trip first, then loads full trip (with days) from Firestore and emits again.
     */
    fun getTripById(tripId: String): Flow<Trip?> = tripDao.getTripById(tripId)
        .map { entity ->
            entity?.let {
                val days = loadDaysForTrip(tripId)
                TripMapper.toDomain(it, days)
            }
        }
        .catch { emit(null) }
        .flowOn(Dispatchers.IO)
        .flatMapLatest { cachedTrip ->
            flow {
                emit(cachedTrip)
                try {
                    when (val result = firestoreDataSource.loadTripWithNestedData(tripId)) {
                        is Result.Success -> {
                            withContext(Dispatchers.IO) {
                                saveTripWithNestedData(result.data)
                            }
                            emit(result.data)
                        }
                        is Result.Error -> throw result.exception
                            ?: Exception(result.message ?: "Failed to load trip")
                        is Result.Loading -> Unit
                    }
                } catch (e: Exception) {
                    // Keep showing the cached trip so the UI doesn't go blank;
                    // re-throw so the outer .catch in the ViewModel can log/show the error.
                    emit(cachedTrip)
                    throw e
                }
            }
        }
    
    /**
     * Create a new trip.
     * Saves to Firestore and caches in Room.
     */
    fun createTrip(trip: Trip): Flow<Result<Trip>> = flow {
        emit(Result.Loading)
        
        try {
            // Save to Firestore
            val firestoreResult = firestoreDataSource.createTrip(trip)
            when (firestoreResult) {
                is Result.Success -> {
                    val tripId = firestoreResult.data.id
                    // createTrip() writes days with generated doc IDs; returned Trip still has empty day ids — reload nested data.
                    val refreshed = firestoreDataSource.loadTripWithNestedData(tripId)
                    when (refreshed) {
                        is Result.Success -> {
                            withContext(Dispatchers.IO) {
                                saveTripWithNestedData(refreshed.data)
                            }
                            emit(Result.Success(refreshed.data))
                        }
                        is Result.Error -> emit(
                            Result.Error(
                                refreshed.exception,
                                refreshed.message ?: "Trip created but failed to load details"
                            )
                        )
                        is Result.Loading -> emit(Result.Loading)
                    }
                }
                is Result.Error -> emit(firestoreResult)
                is Result.Loading -> emit(firestoreResult)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update an existing trip.
     * Updates Firestore and Room cache.
     */
    fun updateTrip(trip: Trip): Flow<Result<Trip>> = flow {
        emit(Result.Loading)
        
        try {
            // Update in Firestore
            val firestoreResult = firestoreDataSource.updateTrip(trip)
            when (firestoreResult) {
                is Result.Success -> {
                    val updatedTrip = firestoreResult.data
                    // Update Room cache
                    withContext(Dispatchers.IO) {
                        saveTripWithNestedData(updatedTrip)
                    }
                    emit(Result.Success(updatedTrip))
                }
                is Result.Error -> emit(firestoreResult)
                is Result.Loading -> emit(firestoreResult)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a trip.
     * Deletes from Firestore and Room cache.
     */
    fun deleteTrip(tripId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        
        try {
            // Delete from Firestore
            val firestoreResult = firestoreDataSource.deleteTrip(tripId)
            when (firestoreResult) {
                is Result.Success -> {
                    // Delete from Room cache
                    withContext(Dispatchers.IO) {
                        val days = tripDayDao.getDaysByTrip(tripId).firstOrNull().orEmpty()
                        days.forEach { day ->
                            tripItemDao.deleteByDayId(day.id)
                        }
                        tripDayDao.deleteByTripId(tripId) // Delete days
                        tripDao.deleteById(tripId) // Delete trip
                    }
                    emit(Result.Success(Unit))
                }
                is Result.Error -> emit(firestoreResult)
                is Result.Loading -> emit(firestoreResult)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Add a day to a trip.
     * Saves to Firestore and Room cache.
     */
    fun addDayToTrip(tripId: String, day: TripDay): Flow<Result<TripDay>> = flow {
        emit(Result.Loading)
        
        try {
            // Get current trip
            val tripResult = firestoreDataSource.loadTripWithNestedData(tripId)
            when (tripResult) {
                is Result.Success -> {
                    val trip = tripResult.data
                    val updatedDays = trip.days + day.copy(tripId = tripId)
                    val updatedTrip = trip.copy(days = updatedDays)
                    
                    // Update trip in Firestore
                    val updateResult = firestoreDataSource.updateTrip(updatedTrip)
                    when (updateResult) {
                        is Result.Success -> {
                            // Firestore assigns real day IDs in saveTripDay; never cache "" id in Room (PRIMARY KEY collision).
                            val refreshed = firestoreDataSource.loadTripWithNestedData(tripId)
                            when (refreshed) {
                                is Result.Success -> {
                                    withContext(Dispatchers.IO) {
                                        saveTripWithNestedData(refreshed.data)
                                    }
                                    val savedDay = refreshed.data.days
                                        .find { it.dayNumber == day.dayNumber && it.tripId == tripId }
                                        ?: refreshed.data.days.lastOrNull()
                                    if (savedDay != null) {
                                        emit(Result.Success(savedDay))
                                    } else {
                                        emit(
                                            Result.Error(
                                                IllegalStateException("Day missing after save"),
                                                "Could not refresh trip after adding day"
                                            )
                                        )
                                    }
                                }
                                is Result.Error -> emit(
                                    Result.Error(refreshed.exception, refreshed.message)
                                )
                                is Result.Loading -> emit(Result.Loading)
                            }
                        }
                        is Result.Error -> emit(updateResult)
                        is Result.Loading -> emit(updateResult)
                    }
                }
                is Result.Error -> emit(Result.Error(tripResult.exception, tripResult.message))
                is Result.Loading -> emit(Result.Loading)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Add an item to a trip day.
     * Saves to Firestore and Room cache.
     */
    fun addItemToDay(tripId: String, dayId: String, item: TripItem): Flow<Result<TripItem>> = flow {
        emit(Result.Loading)
        
        try {
            // Get trip and update
            val tripResult = firestoreDataSource.loadTripWithNestedData(tripId)
            when (tripResult) {
                is Result.Success -> {
                    val trip = tripResult.data
                    val day = trip.days.find { it.id == dayId }
                        ?: run {
                            emit(Result.Error(Exception("Day not found"), "Day not found in trip"))
                            return@flow
                        }
                    
                    val updatedItems = day.items + item.copy(dayId = dayId)
                    val updatedDay = day.copy(items = updatedItems)
                    val updatedDays = trip.days.map { if (it.id == dayId) updatedDay else it }
                    val updatedTrip = trip.copy(days = updatedDays)
                    
                    // Update trip in Firestore
                    val updateResult = firestoreDataSource.updateTrip(updatedTrip)
                    when (updateResult) {
                        is Result.Success -> {
                            // Reload to get the server-assigned item ID (Firestore generates it).
                            val refreshedTripResult = firestoreDataSource.loadTripWithNestedData(tripId)
                            if (refreshedTripResult is Result.Success) {
                                withContext(Dispatchers.IO) {
                                    saveTripWithNestedData(refreshedTripResult.data)
                                }
                                // Find the saved item by matching on postId/placeId so we emit its real id.
                                val savedItem = refreshedTripResult.data.days
                                    .find { it.id == dayId }
                                    ?.items
                                    ?.lastOrNull {
                                        (item.postId != null && it.postId == item.postId) ||
                                        (item.placeId != null && it.placeId == item.placeId)
                                    }
                                emit(Result.Success(savedItem ?: item.copy(dayId = dayId)))
                            } else {
                                emit(Result.Success(item.copy(dayId = dayId)))
                            }
                        }
                        is Result.Error -> emit(updateResult)
                        is Result.Loading -> emit(updateResult)
                    }
                }
                is Result.Error -> emit(Result.Error(tripResult.exception, tripResult.message))
                is Result.Loading -> emit(Result.Loading)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Reorder items in a trip day.
     * Updates Firestore and Room cache.
     */
    fun reorderItems(dayId: String, items: List<TripItem>): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        
        try {
            // Get the day to find trip ID
            val dayEntity = withContext(Dispatchers.IO) {
                tripDayDao.getDayById(dayId)
                    .catch { emit(null) }
                    .firstOrNull()
            }
            
            if (dayEntity == null) {
                emit(Result.Error(Exception("Day not found"), "Day with ID $dayId not found"))
                return@flow
            }
            
            // Get trip and update
            val tripResult = firestoreDataSource.loadTripWithNestedData(dayEntity.tripId)
            when (tripResult) {
                is Result.Success -> {
                    val trip = tripResult.data
                    val day = trip.days.find { it.id == dayId }
                        ?: run {
                            emit(Result.Error(Exception("Day not found"), "Day not found in trip"))
                            return@flow
                        }
                    
                    val updatedDay = day.copy(items = items.mapIndexed { index, item ->
                        item.copy(dayId = dayId, order = index)
                    })
                    val updatedDays = trip.days.map { if (it.id == dayId) updatedDay else it }
                    val updatedTrip = trip.copy(days = updatedDays)
                    
                    // Update trip in Firestore
                    val updateResult = firestoreDataSource.updateTrip(updatedTrip)
                    when (updateResult) {
                        is Result.Success -> {
                            // Update Room cache
                            withContext(Dispatchers.IO) {
                                tripItemDao.deleteByDayId(dayId)
                                tripItemDao.insertAll(
                                    TripItemMapper.toEntityList(updatedDay.items)
                                )
                            }
                            emit(Result.Success(Unit))
                        }
                        is Result.Error -> emit(updateResult)
                        is Result.Loading -> emit(updateResult)
                    }
                }
                is Result.Error -> emit(Result.Error(tripResult.exception, tripResult.message))
                is Result.Loading -> emit(Result.Loading)
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Load days for a trip from Room cache.
     */
    private suspend fun loadDaysForTrip(tripId: String): List<TripDay> {
        val dayEntities = tripDayDao.getDaysByTrip(tripId)
            .catch { emit(emptyList()) }
            .firstOrNull() ?: emptyList()
        
        return dayEntities.map { dayEntity ->
            val items = loadItemsForDay(dayEntity.id)
            TripDayMapper.toDomain(dayEntity, items)
        }
    }
    
    /**
     * Load items for a day from Room cache.
     */
    private suspend fun loadItemsForDay(dayId: String): List<TripItem> {
        val itemEntities = tripItemDao.getItemsByDay(dayId)
            .catch { emit(emptyList()) }
            .firstOrNull() ?: emptyList()
        
        return TripItemMapper.toDomainList(itemEntities)
    }
    
    /**
     * Save a trip with all nested data to Room cache.
     * Removes stale day and item rows that are no longer present in [trip].
     */
    private suspend fun saveTripWithNestedData(trip: Trip) {
        tripDao.insert(TripMapper.toEntity(trip))

        val currentDayIds = trip.days.map { it.id }.toSet()

        // Remove days that are no longer in the trip
        val cachedDays = tripDayDao.getDaysByTrip(trip.id)
            .catch { emit(emptyList()) }
            .firstOrNull() ?: emptyList()
        cachedDays.forEach { dayEntity ->
            if (dayEntity.id !in currentDayIds) {
                tripItemDao.deleteByDayId(dayEntity.id)
                tripDayDao.deleteById(dayEntity.id)
            }
        }

        trip.days.forEach { day ->
            tripDayDao.insert(TripDayMapper.toEntity(day))

            val currentItemIds = day.items.map { it.id }.toSet()

            // Remove items that are no longer in this day
            val cachedItems = tripItemDao.getItemsByDay(day.id)
                .catch { emit(emptyList()) }
                .firstOrNull() ?: emptyList()
            cachedItems.forEach { itemEntity ->
                if (itemEntity.id !in currentItemIds) {
                    tripItemDao.deleteById(itemEntity.id)
                }
            }

            tripItemDao.insertAll(TripItemMapper.toEntityList(day.items))
        }
    }
    
    /**
     * Save multiple trips with nested data to Room cache.
     */
    private suspend fun saveTripsWithNestedData(trips: List<Trip>) {
        trips.forEach { trip ->
            saveTripWithNestedData(trip)
        }
    }
}

