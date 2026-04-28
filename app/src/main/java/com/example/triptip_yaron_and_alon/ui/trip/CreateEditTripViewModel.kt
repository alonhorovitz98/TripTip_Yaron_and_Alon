package com.example.triptip_yaron_and_alon.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.repository.TripRepository
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CreateEditTripViewModel(application: Application) : AndroidViewModel(application) {

    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val tripRepository by lazy {
        TripRepository(
            database.tripDao(),
            database.tripDayDao(),
            database.tripItemDao(),
            firestoreDataSource
        )
    }

    private val _currentTrip = MutableLiveData<Trip?>()
    val currentTrip: LiveData<Trip?> = _currentTrip

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Emits the saved trip on create/update success
    private val _saveResult = MutableLiveData<Trip?>()
    val saveResult: LiveData<Trip?> = _saveResult

    // Emits the day number after a day is successfully added
    private val _dayAdded = MutableLiveData<Int?>()
    val dayAdded: LiveData<Int?> = _dayAdded

    private var loadTripJob: Job? = null

    // ─────────────────── Initialization ───────────────────

    fun initNewTrip() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            _currentTrip.value = Trip(
                id = "new",
                userId = uid,
                title = "",
                description = null,
                createdAt = System.currentTimeMillis(),
                days = emptyList()
            )
        } else {
            viewModelScope.launch {
                val userId = authDataSource.getCurrentUser().firstOrNull()?.id ?: run {
                    _error.value = "User not authenticated"
                    return@launch
                }
                _currentTrip.value = Trip(
                    id = "new",
                    userId = userId,
                    title = "",
                    description = null,
                    createdAt = System.currentTimeMillis(),
                    days = emptyList()
                )
            }
        }
    }

    fun loadTrip(tripId: String) {
        loadTripJob?.cancel()
        loadTripJob = viewModelScope.launch {
            _isLoading.value = true
            var isFirst = true
            tripRepository.getTripById(tripId)
                .catch { e ->
                    _isLoading.value = false
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

    // ─────────────────── Validation ───────────────────

    fun validate(title: String, startDate: Long?, endDate: Long?): String? {
        if (title.isBlank()) return "Trip name is required"
        if (startDate == null) return "Start date is required"
        if (endDate == null) return "End date is required"
        if (endDate <= startDate) return "End date must be after start date"
        return null
    }

    // ─────────────────── Create / Update ───────────────────

    fun createTrip(title: String, description: String?, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: authDataSource.getCurrentUser().firstOrNull()?.id
                ?: run { _error.value = "User not authenticated"; return@launch }

            val trip = Trip(
                id = "",
                userId = userId,
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
                startDate = startDate,
                endDate = endDate,
                days = emptyList()
            )

            tripRepository.createTrip(trip).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                        _saveResult.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to create trip"
                    }
                }
            }
        }
    }

    fun updateTrip(tripId: String, title: String, description: String?, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            val current = tripRepository.getTripById(tripId).firstOrNull() ?: run {
                _error.value = "Trip not found"
                return@launch
            }
            val updated = current.copy(
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                startDate = startDate,
                endDate = endDate
            )
            tripRepository.updateTrip(updated).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                        _saveResult.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to update trip"
                    }
                }
            }
        }
    }

    // ─────────────────── Day management ───────────────────

    fun addDay(tripId: String, dateMillis: Long? = null) {
        viewModelScope.launch {
            val currentTrip = _currentTrip.value ?: run {
                _error.value = "No trip loaded"
                return@launch
            }
            val dayNumber = currentTrip.days.size + 1
            val day = TripDay(
                id = "",
                tripId = tripId,
                dayNumber = dayNumber,
                date = dateMillis,
                items = emptyList()
            )
            tripRepository.addDayToTrip(tripId, day).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        // Patch the live trip directly so the new day appears instantly.
                        // Do NOT call loadTrip() here: a Firestore reload can return fewer
                        // days than Room (e.g. sub-collection rules not set up) which would
                        // overwrite this value and make the day disappear.
                        val current = _currentTrip.value
                        if (current != null) {
                            _currentTrip.value = current.copy(
                                days = (current.days + result.data)
                                    .sortedBy { it.dayNumber }
                            )
                        }
                        _dayAdded.value = dayNumber
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to add day"
                    }
                }
            }
        }
    }

    fun removeDay(tripId: String, dayId: String) {
        viewModelScope.launch {
            val current = tripRepository.getTripById(tripId).firstOrNull() ?: run {
                _error.value = "Trip not found"
                return@launch
            }
            val updated = current.copy(days = current.days.filter { it.id != dayId })
            tripRepository.updateTrip(updated).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to remove day"
                    }
                }
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
}
