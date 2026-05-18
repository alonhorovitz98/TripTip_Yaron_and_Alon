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
 * Handles loading nearby places using Google Places API with pagination support.
 */
class NearbyPlacesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val placeInfoRepository = PlaceInfoRepository()
    
    // LiveData for places (accumulated list)
    private val _places = MutableLiveData<List<PlaceInfo>>()
    val places: LiveData<List<PlaceInfo>> = _places
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Loading more state (for pagination)
    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Next page token for pagination
    private var nextPageToken: String? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    
    // Job tracking to prevent multiple collectors
    private var loadPlacesJob: Job? = null
    private var loadMoreJob: Job? = null
    
    /**
     * Load nearby places for given coordinates (first page).
     */
    fun loadNearbyPlaces(latitude: Double, longitude: Double, radius: Int = 5000) {
        // Cancel existing jobs
        loadPlacesJob?.cancel()
        loadMoreJob?.cancel()
        
        // Reset pagination
        nextPageToken = null
        currentLatitude = latitude
        currentLongitude = longitude
        
        loadPlacesJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _places.value = emptyList()
            
            placeInfoRepository.getGoogleNearbyPlaces(latitude, longitude, radius, null)
                .catch { e ->
                    _error.value = "Failed to load nearby places: ${e.message}"
                    _isLoading.value = false
                }
                .collect { result ->
                    _places.value = result.places
                    nextPageToken = result.nextPageToken
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Load more places (next page).
     */
    fun loadMorePlaces() {
        val token = nextPageToken
        val lat = currentLatitude
        val lon = currentLongitude
        
        if (token == null || lat == null || lon == null) return
        if (loadMoreJob?.isActive == true) return
        
        loadMoreJob = viewModelScope.launch {
            _isLoadingMore.value = true
            
            placeInfoRepository.getGoogleNearbyPlaces(lat, lon, 5000, token)
                .catch { e ->
                    _error.value = "Failed to load more places: ${e.message}"
                    _isLoadingMore.value = false
                }
                .collect { result ->
                    val currentPlaces = _places.value ?: emptyList()
                    _places.value = currentPlaces + result.places
                    nextPageToken = result.nextPageToken
                    _isLoadingMore.value = false
                }
        }
    }
    
    /**
     * Check if there are more pages to load.
     */
    fun hasMorePages(): Boolean = nextPageToken != null
    
    /**
     * Refresh nearby places (reset and reload first page).
     */
    fun refreshPlaces(latitude: Double, longitude: Double) {
        loadPlacesJob?.cancel()
        loadMoreJob?.cancel()
        loadPlacesJob = null
        loadMoreJob = null
        loadNearbyPlaces(latitude, longitude)
    }
}
