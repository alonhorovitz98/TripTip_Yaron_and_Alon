package com.example.triptip_yaron_and_alon.ui.place

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.repository.PlaceInfoRepository
import com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for Google Place Details screen.
 * Handles loading full place details including photos, reviews, opening hours, etc.
 */
class GooglePlaceDetailsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val placeInfoRepository = PlaceInfoRepository()
    
    // LiveData for place details
    private val _placeDetails = MutableLiveData<PlaceDetailsResultDto?>()
    val placeDetails: LiveData<PlaceDetailsResultDto?> = _placeDetails
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Job tracking to prevent multiple collectors
    private var loadDetailsJob: Job? = null
    
    /**
     * Load full place details by place_id.
     */
    fun loadPlaceDetails(placeId: String) {
        // Cancel existing job if active
        loadDetailsJob?.cancel()
        
        android.util.Log.d("PlaceDetailsVM", "Loading details for placeId=$placeId")
        
        loadDetailsJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _placeDetails.value = null
            
            placeInfoRepository.getFullPlaceDetails(placeId)
                .catch { e ->
                    android.util.Log.e("PlaceDetailsVM", "Error loading place details: ${e.message}", e)
                    _error.value = "Failed to load place details: ${e.message}"
                    _isLoading.value = false
                }
                .collect { details ->
                    android.util.Log.d("PlaceDetailsVM", "Loaded place details: ${details.name}")
                    _placeDetails.value = details
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Clear place details (e.g., when navigating away).
     */
    fun clearDetails() {
        loadDetailsJob?.cancel()
        _placeDetails.value = null
        _error.value = null
    }
}
