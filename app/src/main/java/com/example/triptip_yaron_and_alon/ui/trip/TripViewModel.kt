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
import com.example.triptip_yaron_and_alon.data.repository.PlaceInfoRepository
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.data.repository.TripRepository
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TripViewModel(application: Application) : AndroidViewModel(application) {
    
    // Use lazy initialization to defer heavy object creation
    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val storageDataSource by lazy { FirebaseStorageDataSource(application) }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val tripRepository by lazy {
        TripRepository(
            database.tripDao(),
            database.tripDayDao(),
            database.tripItemDao(),
            firestoreDataSource
        )
    }
    private val postRepository by lazy {
        PostRepository(
            database.postDao(),
            firestoreDataSource,
            storageDataSource
        )
    }
    private val placeInfoRepository by lazy { PlaceInfoRepository() }
    
    // User trips
    private val _userTrips = MutableLiveData<List<Trip>>()
    val userTrips: LiveData<List<Trip>> = _userTrips
    
    // Current trip
    private val _currentTrip = MutableLiveData<Trip?>()
    val currentTrip: LiveData<Trip?> = _currentTrip
    
    // Current day
    private val _currentDay = MutableLiveData<TripDay?>()
    val currentDay: LiveData<TripDay?> = _currentDay
    
    // Available posts (for adding to day)
    private val _availablePosts = MutableLiveData<List<Post>>()
    val availablePosts: LiveData<List<Post>> = _availablePosts
    
    // Nearby places (for adding to day)
    private val _nearbyPlaces = MutableLiveData<List<PlaceInfo>>()
    val nearbyPlaces: LiveData<List<PlaceInfo>> = _nearbyPlaces
    
    private val _placesLoading = MutableLiveData<Boolean>()
    val placesLoading: LiveData<Boolean> = _placesLoading
    
    private val _placesError = MutableLiveData<String?>()
    val placesError: LiveData<String?> = _placesError

    // Place search (Google Autocomplete) for Day Editor
    private val _placeSearchSuggestions = MutableLiveData<List<LocationSuggestion>>(emptyList())
    val placeSearchSuggestions: LiveData<List<LocationSuggestion>> = _placeSearchSuggestions
    
    // Item operation result
    private val _itemOperationResult = MutableLiveData<Result<Unit>>()
    val itemOperationResult: LiveData<Result<Unit>> = _itemOperationResult
    
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
    
    // Job tracking to prevent multiple collectors
    private var loadUserTripsJob: Job? = null
    private var loadTripJob: Job? = null
    private var loadDayJob: Job? = null
    private var loadAvailablePostsJob: Job? = null
    
    fun loadUserTrips(userId: String? = null) {
        // Allow explicit reload requests by cancelling any existing collector.
        loadUserTripsJob?.cancel()
        
        loadUserTripsJob = viewModelScope.launch {
            // Get current user ID from Firebase Auth if not provided
            val actualUserId = userId ?: run {
                authDataSource.getCurrentUser().firstOrNull()?.id
                    ?: run {
                        _error.value = "User not authenticated"
                        _isLoading.value = false
                        return@launch
                    }
            }
            
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            tripRepository.getTrips(actualUserId).collect { trips ->
                _userTrips.value = trips
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun loadTrip(tripId: String) {
        // Cancel existing job if active to allow reloading (e.g., after adding a day)
        loadTripJob?.cancel()
        
        loadTripJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            tripRepository.getTripById(tripId).collect { trip ->
                _currentTrip.value = trip
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun createTrip(title: String, description: String?, userId: String? = null, startDate: Long? = null, endDate: Long? = null) {
        if (title.isBlank()) {
            _error.value = "Trip title cannot be empty"
            return
        }
        
        viewModelScope.launch {
            // Get current user ID from Firebase Auth
            val actualUserId = userId ?: run {
                authDataSource.getCurrentUser().firstOrNull()?.id
                    ?: run {
                        _error.value = "User not authenticated"
                        return@launch
                    }
            }
            
            // Get days from current trip if it exists (for new trips with days added before saving)
            val existingDays = _currentTrip.value?.days?.filter { it.id.startsWith("temp_") }?.map { day ->
                // Convert temporary days to proper format (remove temp_ prefix from ID)
                day.copy(id = "") // Will be generated by Firestore
            } ?: emptyList()
            
            // Create Trip object with days
            val trip = Trip(
                id = "", // Will be generated by Firestore
                userId = actualUserId,
                title = title,
                description = description,
                createdAt = System.currentTimeMillis(),
                startDate = startDate,
                endDate = endDate,
                days = existingDays // Include days that were added before saving
            )
            
            tripRepository.createTrip(trip).collect { result ->
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
    
    /**
     * Initialize a new trip object (for adding days before saving).
     * Uses [FirebaseAuth.getInstance].currentUser when available so the trip is ready immediately
     * (avoids "Add Day" before the auth Flow emits).
     */
    fun initializeNewTrip() {
        val uidImmediate = FirebaseAuth.getInstance().currentUser?.uid
        if (uidImmediate != null) {
            _currentTrip.value = Trip(
                id = "new",
                userId = uidImmediate,
                title = "",
                description = null,
                createdAt = System.currentTimeMillis(),
                days = emptyList()
            )
            return
        }
        viewModelScope.launch {
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id
                ?: run {
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
    
    /**
     * Add day to local trip (for new trips before saving).
     */
    fun addDayToLocalTrip(dayNumber: Int, description: String) {
        val currentTrip = _currentTrip.value ?: run {
            _error.value = "Trip not initialized"
            return
        }
        
        val newDay = TripDay(
            id = "temp_${System.currentTimeMillis()}", // Temporary ID
            tripId = "new",
            dayNumber = dayNumber,
            date = null,
            items = emptyList()
        )
        
        val updatedDays = currentTrip.days + newDay
        val updatedTrip = currentTrip.copy(days = updatedDays)
        _currentTrip.value = updatedTrip
    }
    
    fun updateTrip(tripId: String, title: String, description: String?, startDate: Long? = null, endDate: Long? = null) {
        if (title.isBlank()) {
            _error.value = "Trip title cannot be empty"
            return
        }
        
        viewModelScope.launch {
            val currentTrip = tripRepository.getTripById(tripId).firstOrNull()
                ?: run {
                    _error.value = "Trip not found"
                    return@launch
                }
            
            val updatedTrip = currentTrip.copy(
                title = title,
                description = description,
                startDate = startDate ?: currentTrip.startDate,
                endDate = endDate ?: currentTrip.endDate
            )
            
            tripRepository.updateTrip(updatedTrip).collect { result ->
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
        android.util.Log.d("TripViewModel", "addDay called - tripId: $tripId, dayNumber: $dayNumber")
        viewModelScope.launch {
            // Create TripDay object
            val day = TripDay(
                id = "", // Will be generated by Firestore
                tripId = tripId,
                dayNumber = dayNumber,
                date = null,
                items = emptyList()
            )
            
            tripRepository.addDayToTrip(tripId, day).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        android.util.Log.d("TripViewModel", "addDay - Loading...")
                        _isLoading.value = true
                    }
                    is Result.Success -> {
                        android.util.Log.d("TripViewModel", "addDay - Success! Reloading trip $tripId...")
                        _isLoading.value = false
                        // Reload trip to get updated days
                        loadTrip(tripId)
                    }
                    is Result.Error -> {
                        android.util.Log.e("TripViewModel", "addDay - Error: ${result.message}", result.exception)
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }
    
    fun updateDay(tripId: String, dayId: String, dayNumber: Int, description: String) {
        viewModelScope.launch {
            // Get current trip
            val currentTrip = tripRepository.getTripById(tripId).firstOrNull()
                ?: run {
                    _error.value = "Trip not found"
                    return@launch
                }
            
            // Update the day in the trip's days list
            val updatedDays = currentTrip.days.map { day ->
                if (day.id == dayId) {
                    day.copy(dayNumber = dayNumber)
                } else {
                    day
                }
            }
            
            val updatedTrip = currentTrip.copy(days = updatedDays)
            
            tripRepository.updateTrip(updatedTrip).collect { result ->
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

    /**
     * Update a day's date.
     */
    fun updateDayDate(tripId: String, dayId: String, dateMillis: Long?) {
        viewModelScope.launch {
            val currentTrip = tripRepository.getTripById(tripId).firstOrNull()
                ?: run {
                    _error.value = "Trip not found"
                    return@launch
                }
            val updatedDays = currentTrip.days.map { day ->
                if (day.id == dayId) day.copy(date = dateMillis) else day
            }
            val updatedTrip = currentTrip.copy(days = updatedDays)
            tripRepository.updateTrip(updatedTrip).collect { result ->
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
            // Get current trip
            val currentTrip = tripRepository.getTripById(tripId).firstOrNull()
                ?: run {
                    _error.value = "Trip not found"
                    return@launch
                }
            
            // Remove the day from the trip's days list
            val updatedDays = currentTrip.days.filter { it.id != dayId }
            val updatedTrip = currentTrip.copy(days = updatedDays)
            
            tripRepository.updateTrip(updatedTrip).collect { result ->
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
            // Get current trip
            val currentTrip = tripRepository.getTripById(tripId).firstOrNull()
                ?: run {
                    _error.value = "Trip not found"
                    return@launch
                }
            
            // Update trip with reordered days
            val updatedTrip = currentTrip.copy(days = reorderedDays)
            
            tripRepository.updateTrip(updatedTrip).collect { result ->
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
    
    // ==================== DAY ITEM MANAGEMENT ====================
    
    fun loadDay(tripId: String, dayId: String) {
        // Allow explicit reload requests after add/remove actions.
        loadDayJob?.cancel()
        
        loadDayJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            tripRepository.getTripById(tripId).collect { trip ->
                if (trip != null) {
                    val day = trip.days.find { it.id == dayId }
                    if (day != null) {
                        // Load linked data for items (post/place) when possible.
                        val enrichedItems = day.items.map { item ->
                            if (item.postId != null) {
                                val post = postRepository.getPostById(item.postId).firstOrNull()
                                item.copy(post = post)
                            } else if (!item.placeId.isNullOrBlank()) {
                                val place = try {
                                    placeInfoRepository.getPlaceInfoFromGooglePlaceId(item.placeId).firstOrNull()
                                } catch (_: Exception) {
                                    null
                                }
                                item.copy(place = place)
                            } else {
                                item
                            }
                        }
                        _currentDay.value = day.copy(items = enrichedItems)
                    } else {
                        _currentDay.value = null
                    }
                    _currentTrip.value = trip
                }
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun loadAvailablePosts() {
        // Cancel existing job if active to prevent duplicate collectors
        if (loadAvailablePostsJob?.isActive == true) {
            return
        }
        
        loadAvailablePostsJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            postRepository.getPosts().collect { posts ->
                _availablePosts.value = posts
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun addItemToDay(tripId: String, dayId: String, postId: String) {
        viewModelScope.launch {
            val currentDay = _currentDay.value
            if (currentDay == null) {
                _error.value = "Day not loaded"
                return@launch
            }
            
            // Check if post is already in day
            if (currentDay.items.any { it.postId != null && it.postId == postId }) {
                _error.value = "Post is already in this day"
                return@launch
            }
            
            // Create new item
            val newOrder = currentDay.items.size
            val newItem = TripItem(
                id = "", // Will be generated by Firestore
                dayId = dayId,
                postId = postId,
                placeId = null,
                order = newOrder,
                notes = null
            )
            
            tripRepository.addItemToDay(tripId, dayId, newItem).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _itemOperationResult.value = Result.Success(Unit)
                        // Reload day
                        val tripId = _currentTrip.value?.id ?: return@collect
                        loadDay(tripId, dayId)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _itemOperationResult.value = result
                    }
                }
            }
        }
    }
    
    /**
     * Add a place to a trip day.
     */
    fun addPlaceToDay(tripId: String, dayId: String, place: PlaceInfo) {
        viewModelScope.launch {
            val currentDay = _currentDay.value
            if (currentDay == null) {
                _error.value = "Day not loaded"
                return@launch
            }
            
            // Check if place is already in day
            if (currentDay.items.any { it.placeId == place.xid }) {
                _error.value = "Place is already in this day"
                return@launch
            }
            
            // Create new item
            val newOrder = currentDay.items.size
            val newItem = TripItem(
                id = "", // Will be generated by Firestore
                dayId = dayId,
                postId = null,
                placeId = place.xid,
                order = newOrder,
                notes = null,
                place = place
            )
            
            tripRepository.addItemToDay(tripId, dayId, newItem).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _itemOperationResult.value = Result.Success(Unit)
                        // Reload day
                        val tripId = _currentTrip.value?.id ?: return@collect
                        loadDay(tripId, dayId)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _itemOperationResult.value = result
                    }
                }
            }
        }
    }
    
    /**
     * Search for places by query (Google Places Autocomplete) for adding to day.
     */
    fun searchPlaceSuggestions(query: String) {
        if (query.isBlank()) {
            _placeSearchSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val suggestions = placeInfoRepository.searchLocationSuggestions(query).firstOrNull() ?: emptyList()
                _placeSearchSuggestions.postValue(suggestions)
            } catch (e: Exception) {
                _placeSearchSuggestions.postValue(emptyList())
            }
        }
    }

    /**
     * Add a place to day by Google place_id (from search). Fetches full details then adds.
     */
    fun addGooglePlaceToDay(tripId: String, dayId: String, googlePlaceId: String) {
        viewModelScope.launch {
            _placesLoading.value = true
            _placesError.value = null
            try {
                placeInfoRepository.getPlaceInfoFromGooglePlaceId(googlePlaceId)
                    .collect { place ->
                        _placesLoading.value = false
                        addPlaceToDay(tripId, dayId, place)
                    }
            } catch (e: Exception) {
                _placesLoading.value = false
                _placesError.value = "Failed to add place: ${e.message}"
                _itemOperationResult.value = Result.Error(e, e.message)
            }
        }
    }

    /**
     * Load nearby places for a location (latitude, longitude). Uses Google Places for better results.
     */
    fun loadNearbyPlaces(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _placesLoading.value = true
            _placesError.value = null
            try {
                placeInfoRepository.getGoogleNearbyPlaces(latitude, longitude, 5000, null)
                    .catch { e ->
                        _placesLoading.value = false
                        _placesError.value = "Failed to load places: ${e.message}"
                    }
                    .collect { result ->
                        _nearbyPlaces.value = result.places
                        _placesLoading.value = false
                    }
            } catch (e: Exception) {
                _placesLoading.value = false
                _placesError.value = "Failed to load places: ${e.message}"
            }
        }
    }
    
    /**
     * Load nearby places for a location name (geocodes first, then loads places).
     */
    fun loadNearbyPlacesForLocation(locationName: String) {
        viewModelScope.launch {
            _placesLoading.value = true
            _placesError.value = null
            
            try {
                val coordinates = placeInfoRepository.geocodeLocation(locationName)
                    .firstOrNull()
                
                if (coordinates != null) {
                    loadNearbyPlaces(coordinates.first, coordinates.second)
                } else {
                    _placesLoading.value = false
                    _placesError.value = "Location not found"
                }
            } catch (e: Exception) {
                _placesLoading.value = false
                _placesError.value = "Failed to geocode location: ${e.message}"
            }
        }
    }
    
    fun updateItemNotes(dayId: String, itemId: String, notes: String?) {
        viewModelScope.launch {
            val currentTrip = _currentTrip.value
            val currentDay = _currentDay.value
            if (currentTrip == null || currentDay == null) {
                _error.value = "Day not loaded"
                return@launch
            }
            
            // Update item in day
            val updatedItems = currentDay.items.map { item ->
                if (item.id == itemId) {
                    item.copy(notes = notes)
                } else {
                    item
                }
            }
            val updatedDay = currentDay.copy(items = updatedItems)
            val updatedDays = currentTrip.days.map { if (it.id == dayId) updatedDay else it }
            val updatedTrip = currentTrip.copy(days = updatedDays)
            
            tripRepository.updateTrip(updatedTrip).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentDay.value = updatedDay
                        _currentTrip.value = result.data
                        _itemOperationResult.value = Result.Success(Unit)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _itemOperationResult.value = result
                    }
                }
            }
        }
    }
    
    fun removeItemFromDay(dayId: String, itemId: String) {
        viewModelScope.launch {
            val currentTrip = _currentTrip.value
            val currentDay = _currentDay.value
            if (currentTrip == null || currentDay == null) {
                _error.value = "Day not loaded"
                return@launch
            }
            
            // Remove item from day
            val updatedItems = currentDay.items.filter { it.id != itemId }
                .mapIndexed { index, item -> item.copy(order = index) }
            val updatedDay = currentDay.copy(items = updatedItems)
            val updatedDays = currentTrip.days.map { if (it.id == dayId) updatedDay else it }
            val updatedTrip = currentTrip.copy(days = updatedDays)
            
            tripRepository.updateTrip(updatedTrip).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentDay.value = updatedDay
                        _currentTrip.value = result.data
                        _itemOperationResult.value = Result.Success(Unit)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _itemOperationResult.value = result
                    }
                }
            }
        }
    }
    
    fun reorderItems(dayId: String, reorderedItems: List<TripItem>) {
        viewModelScope.launch {
            tripRepository.reorderItems(dayId, reorderedItems).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _itemOperationResult.value = result
                        // Reload day
                        val tripId = _currentTrip.value?.id ?: return@collect
                        loadDay(tripId, dayId)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _itemOperationResult.value = result
                    }
                }
            }
        }
    }
}
