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

import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TripListViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _trips = MutableLiveData<List<Trip>>(emptyList())
    val trips: LiveData<List<Trip>> = _trips

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private var loadJob: Job? = null

    fun loadTrips() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id ?: run {
                _error.value = "User not authenticated"
                return@launch
            }
            _isLoading.value = true
            var isFirst = true
            tripRepository.getTrips(userId)
                .catch { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Failed to load trips"
                }
                .collect { trips ->
                    _trips.value = trips
                    if (isFirst) {
                        _isLoading.value = false
                        isFirst = false
                    }
                }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _deleteSuccess.value = true
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to delete trip"
                    }
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
