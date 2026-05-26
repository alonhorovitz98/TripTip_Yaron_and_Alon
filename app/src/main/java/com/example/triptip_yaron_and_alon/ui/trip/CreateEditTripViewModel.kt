package com.example.triptip_yaron_and_alon.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.data.repository.TripsRepository
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class CreateEditTripViewModel(application: Application) : AndroidViewModel(application) {

    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val firestore by lazy { FirestoreDataSource() }
    private val storage by lazy { FirebaseStorageDataSource(application) }
    private val postRepository by lazy { PostRepository(database.postDao(), firestore, storage) }
    private val trips by lazy { TripsRepository(firestore, postRepository) }

    private val _currentTrip = MutableLiveData<Trip?>()
    val currentTrip: LiveData<Trip?> = _currentTrip

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveResult = MutableLiveData<Trip?>()
    val saveResult: LiveData<Trip?> = _saveResult

    private val _dayAdded = MutableLiveData<Int?>()
    val dayAdded: LiveData<Int?> = _dayAdded

    private var loadTripJob: Job? = null

    fun initNewTrip() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            _currentTrip.value = Trip(
                id = "new",
                userId = uid,
                name = ""
            )
            return
        }
        viewModelScope.launch {
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id ?: run {
                _error.value = "User not authenticated"
                return@launch
            }
            _currentTrip.value = Trip(
                id = "new",
                userId = userId,
                name = ""
            )
        }
    }

    fun loadTrip(tripId: String) {
        loadTripJob?.cancel()
        loadTripJob = viewModelScope.launch {
            _isLoading.value = true
            var isFirst = true
            trips.observeTrip(tripId)
                .catch { e ->
                    if (isFirst) _isLoading.value = false
                    _error.value = e.message ?: "Failed to load trip"
                }
                .collect { trip ->
                    _currentTrip.value = trip
                    if (isFirst) {
                        _isLoading.value = false
                        isFirst = false
                    }
                }
        }
    }

    fun validateName(name: String): String? =
        if (name.isBlank()) "Trip name is required" else null

    fun createTrip(name: String, startDateMillis: Long? = null, endDateMillis: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: authDataSource.getCurrentUser().firstOrNull()?.id
                ?: run {
                    _error.value = "User not authenticated"
                    _isLoading.value = false
                    return@launch
                }
            try {
                val id = trips.createTrip(userId, name, startDateMillis, endDateMillis)
                syncDaysIfNeeded(id, startDateMillis, endDateMillis)
                loadTrip(id)
                _saveResult.value = trips.getTrip(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create trip"
                _isLoading.value = false
            }
        }
    }

    fun updateTrip(
        tripId: String,
        name: String,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                trips.updateTrip(tripId, name, startDateMillis, endDateMillis)
                syncDaysIfNeeded(tripId, startDateMillis, endDateMillis)
                loadTrip(tripId)
                _saveResult.value = trips.getTrip(tripId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update trip"
                _isLoading.value = false
            }
        }
    }

    fun addDay(tripId: String, dateMillis: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resolvedDate = dateMillis ?: computeNextDayDate(_currentTrip.value)
                trips.addDay(tripId, resolvedDate)
                _dayAdded.value = trips.getTrip(tripId)?.days?.size
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add day"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeDay(tripId: String, dayId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                trips.removeDay(tripId, dayId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove day"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    fun clearDayAdded() {
        _dayAdded.value = null
    }

    private suspend fun syncDaysIfNeeded(
        tripId: String,
        startDateMillis: Long?,
        endDateMillis: Long?
    ) {
        if (startDateMillis != null && endDateMillis != null && endDateMillis >= startDateMillis) {
            trips.syncDaysForDateRange(tripId, startDateMillis, endDateMillis)
        }
    }

    private fun computeNextDayDate(trip: Trip?): Long? {
        if (trip == null) return null
        val start = trip.startDateMillis ?: return null
        val end = trip.endDateMillis ?: return null
        val index = trip.days.size
        val count = daysBetweenInclusive(start, end)
        if (index >= count) return null
        return dateMillisForDayIndex(start, index)
    }

    private fun daysBetweenInclusive(startMillis: Long, endMillis: Long): Int {
        val zone = ZoneOffset.UTC
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        if (end.isBefore(start)) return 0
        return ChronoUnit.DAYS.between(start, end).toInt() + 1
    }

    private fun dateMillisForDayIndex(startMillis: Long, index: Int): Long {
        val zone = ZoneOffset.UTC
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        return start.plusDays(index.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
