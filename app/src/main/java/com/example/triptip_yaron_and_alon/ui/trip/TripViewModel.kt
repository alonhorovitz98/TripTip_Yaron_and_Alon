package com.example.triptip_yaron_and_alon.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.repository.TripRepository
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.launch

class TripViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firestoreDataSource = FirestoreDataSource()
    private val storageDataSource = FirebaseStorageDataSource()
    private val database = TripTipDatabase.getDatabase(application)
    private val tripRepository = TripRepository(
        firestoreDataSource,
        storageDataSource,
        database.tripDao()
    )
    
    // User trips
    private val _userTrips = MutableLiveData<List<Trip>>()
    val userTrips: LiveData<List<Trip>> = _userTrips
    
    // Current trip
    private val _currentTrip = MutableLiveData<Trip?>()
    val currentTrip: LiveData<Trip?> = _currentTrip
    
    // Operation result
    private val _operationResult = MutableLiveData<Result<Trip>>()
    val operationResult: LiveData<Result<Trip>> = _operationResult
    
    // Delete result
    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult
    
    // Loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadUserTrips(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            tripRepository.getUserTrips(userId).collect { trips ->
                _userTrips.value = trips
                _isLoading.value = false
            }
        }
    }
    
    fun loadTrip(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            tripRepository.getTripById(tripId).collect { trip ->
                _currentTrip.value = trip
                _isLoading.value = false
            }
        }
    }
    
    fun createTrip(title: String, description: String?, startDate: Long?, endDate: Long?) {
        if (title.isBlank()) {
            _error.value = "Trip title cannot be empty"
            return
        }
        
        viewModelScope.launch {
            tripRepository.createTrip(title, description, startDate, endDate).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _operationResult.value = result
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _operationResult.value = result
                    }
                }
            }
        }
    }
    
    fun updateTrip(tripId: String, title: String, description: String?) {
        if (title.isBlank()) {
            _error.value = "Trip title cannot be empty"
            return
        }
        
        viewModelScope.launch {
            tripRepository.updateTrip(tripId, title, description).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _operationResult.value = result
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _operationResult.value = result
                    }
                }
            }
        }
    }
    
    fun addDay(tripId: String, dayNumber: Int, description: String) {
        viewModelScope.launch {
            tripRepository.addDay(tripId, dayNumber, description).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }
    
    fun updateDay(tripId: String, dayId: String, dayNumber: Int, description: String) {
        viewModelScope.launch {
            tripRepository.updateDay(tripId, dayId, dayNumber, description).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }
    
    fun removeDay(tripId: String, dayId: String) {
        viewModelScope.launch {
            tripRepository.removeDay(tripId, dayId).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }
    
    fun reorderDays(tripId: String, reorderedDays: List<TripDay>) {
        viewModelScope.launch {
            tripRepository.reorderDays(tripId, reorderedDays).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
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
                        _deleteResult.value = result
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _deleteResult.value = result
                    }
                }
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
