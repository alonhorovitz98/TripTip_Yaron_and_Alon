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
import com.example.triptip_yaron_and_alon.data.repository.TripRepository
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.Trip
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Job
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
        // Cancel existing job if active to prevent duplicate collectors
        if (loadUserTripsJob?.isActive == true) {
            return
        }
        
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
        // Cancel existing job if active to prevent duplicate collectors
        if (loadTripJob?.isActive == true) {
            return
        }
        
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
            
            // Create Trip object
            val trip = Trip(
                id = "", // Will be generated by Firestore
                userId = actualUserId,
                title = title,
                description = description,
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
            // Get current trip
            val currentTrip = tripRepository.getTripById(tripId).firstOrNull()
                ?: run {
                    _error.value = "Trip not found"
                    return@launch
                }
            
            // Create updated Trip object
            val updatedTrip = currentTrip.copy(
                title = title,
                description = description
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
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        // Reload trip to get updated days
                        loadTrip(tripId)
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
        // Cancel existing job if active to prevent duplicate collectors
        if (loadDayJob?.isActive == true) {
            return
        }
        
        loadDayJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            tripRepository.getTripById(tripId).collect { trip ->
                if (trip != null) {
                    val day = trip.days.find { it.id == dayId }
                    if (day != null) {
                        // Load post details for items
                        val itemsWithPosts = day.items.map { item ->
                            val post = postRepository.getPostById(item.postId).firstOrNull()
                            item.copy(post = post)
                        }
                        val dayWithPosts = day.copy(items = itemsWithPosts)
                        _currentDay.value = dayWithPosts
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
    
    fun addItemToDay(dayId: String, postId: String) {
        viewModelScope.launch {
            val currentDay = _currentDay.value
            if (currentDay == null) {
                _error.value = "Day not loaded"
                return@launch
            }
            
            // Check if post is already in day
            if (currentDay.items.any { it.postId == postId }) {
                _error.value = "Post is already in this day"
                return@launch
            }
            
            // Create new item
            val newOrder = currentDay.items.size
            val newItem = TripItem(
                id = "", // Will be generated by Firestore
                dayId = dayId,
                postId = postId,
                order = newOrder,
                notes = null
            )
            
            tripRepository.addItemToDay(dayId, newItem).collect { result ->
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
