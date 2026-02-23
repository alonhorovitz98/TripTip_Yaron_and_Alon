package com.example.triptip_yaron_and_alon.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.repository.PlaceInfoRepository
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for Nearby Places tab.
 * Handles loading nearby places using Google Places API.
 */
class NearbyPlacesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val placeInfoRepository = PlaceInfoRepository()
    
    // LiveData for places
    private val _places = MutableLiveData<List<PlaceInfo>>()
    val places: LiveData<List<PlaceInfo>> = _places
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Job tracking to prevent multiple collectors
    private var loadPlacesJob: Job? = null
    
    /**
     * Load nearby places for given coordinates.
     */
    fun loadNearbyPlaces(latitude: Double, longitude: Double, radius: Int = 5000) {
        // Cancel existing job if active
        loadPlacesJob?.cancel()
        
        loadPlacesJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            placeInfoRepository.getGoogleNearbyPlaces(latitude, longitude, radius)
                .catch { e ->
                    _error.value = "Failed to load nearby places: ${e.message}"
                    _isLoading.value = false
                }
                .collect { placesList ->
                    _places.value = placesList
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Refresh nearby places.
     */
    fun refreshPlaces(latitude: Double, longitude: Double) {
        loadPlacesJob?.cancel()
        loadPlacesJob = null
        loadNearbyPlaces(latitude, longitude)
    }
}
