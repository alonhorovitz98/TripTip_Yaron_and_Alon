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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TripListViewModel(application: Application) : AndroidViewModel(application) {

    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val firestore by lazy { FirestoreDataSource() }
    private val storage by lazy { FirebaseStorageDataSource(application) }
    private val postRepository by lazy { PostRepository(database.postDao(), firestore, storage) }
    private val tripsRepository by lazy { TripsRepository(firestore, postRepository) }

    private val _trips = MutableLiveData<List<Trip>>(emptyList())
    val trips: LiveData<List<Trip>> = _trips

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _deleteSuccess = MutableLiveData(false)
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
            tripsRepository.observeTripsForUser(userId)
                .catch { e ->
                    if (isFirst) _isLoading.value = false
                    _error.value = e.message ?: "Failed to load trips"
                }
                .collect { list ->
                    _trips.value = list
                    if (isFirst) {
                        _isLoading.value = false
                        isFirst = false
                    }
                }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tripsRepository.deleteTrip(tripId)
                _deleteSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete trip"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
