package com.example.triptip_yaron_and_alon.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.repository.PlaceInfoRepository
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.data.repository.TripRepository
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class DayEditorViewModel(application: Application) : AndroidViewModel(application) {

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
        PostRepository(database.postDao(), firestoreDataSource, storageDataSource)
    }
    private val placeInfoRepository by lazy { PlaceInfoRepository() }

    // ─────────────────── State ───────────────────

    private val _currentTrip = MutableLiveData<com.example.triptip_yaron_and_alon.domain.model.Trip?>()

    private val _currentDay = MutableLiveData<TripDay?>()
    val currentDay: LiveData<TripDay?> = _currentDay

    private val _availablePosts = MutableLiveData<List<Post>>(emptyList())
    val availablePosts: LiveData<List<Post>> = _availablePosts

    private val _placeSuggestions = MutableLiveData<List<LocationSuggestion>>(emptyList())
    val placeSuggestions: LiveData<List<LocationSuggestion>> = _placeSuggestions

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPlaceLoading = MutableLiveData(false)
    val isPlaceLoading: LiveData<Boolean> = _isPlaceLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _itemAdded = MutableLiveData<Boolean>()
    val itemAdded: LiveData<Boolean> = _itemAdded

    private val _daySaved = MutableLiveData<Boolean>()
    val daySaved: LiveData<Boolean> = _daySaved

    private var loadDayJob: Job? = null
    private var postsJob: Job? = null

    // ─────────────────── Load day ───────────────────

    fun loadDay(tripId: String, dayId: String) {
        loadDayJob?.cancel()
        loadDayJob = viewModelScope.launch {
            _isLoading.value = true
            var isFirst = true
            tripRepository.getTripById(tripId)
                .catch { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Failed to load day"
                }
                .collect { trip ->
                    if (trip != null) {
                        _currentTrip.value = trip
                        val day = trip.days.find { it.id == dayId }
                        if (day != null) {
                            val enriched = enrichItems(day)
                            _currentDay.value = enriched
                        } else {
                            _currentDay.value = null
                        }
                    }
                    if (isFirst) {
                        _isLoading.value = false
                        isFirst = false
                    }
                }
        }
    }

    private suspend fun enrichItems(day: TripDay): TripDay {
        val enrichedItems = coroutineScope {
            day.items.map { item ->
                async {
                    when {
                        item.postId != null -> {
                            val post = postRepository.getPostById(item.postId).firstOrNull()
                            item.copy(post = post)
                        }
                        !item.placeId.isNullOrBlank() -> {
                            val place = try {
                                placeInfoRepository.getPlaceInfoFromGooglePlaceId(item.placeId).firstOrNull()
                            } catch (_: Exception) {
                                null
                            }
                            item.copy(place = place)
                        }
                        else -> item
                    }
                }
            }.awaitAll()
        }
        return day.copy(items = enrichedItems)
    }

    private fun getDayNotLoadedMessage() =
        "Day not loaded. Open this day again or wait a moment, then try again."

    private suspend fun requireDay(tripId: String, dayId: String): TripDay? {
        _currentDay.value?.takeIf { it.id == dayId }?.let { return it }
        return try {
            withTimeout(20_000) {
                val trip = tripRepository.getTripById(tripId).first { t ->
                    t?.days?.any { d -> d.id == dayId } == true
                } ?: return@withTimeout null
                val raw = trip.days.find { it.id == dayId } ?: return@withTimeout null
                val enriched = enrichItems(raw)
                _currentTrip.value = trip
                _currentDay.value = enriched
                enriched
            }
        } catch (_: TimeoutCancellationException) {
            null
        } catch (_: NoSuchElementException) {
            null
        } catch (e: Exception) {
            android.util.Log.w("DayEditor", "requireDay: ${e.message}")
            null
        }
    }

    // ─────────────────── Posts ───────────────────

    fun loadAvailablePosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            var isFirst = true
            postRepository.getPosts()
                .catch { e ->
                    _error.value = e.message ?: "Failed to load posts"
                }
                .collect { posts ->
                    _availablePosts.value = posts
                    if (isFirst) isFirst = false
                }
        }
    }

    fun addPostToDay(tripId: String, dayId: String, post: Post) {
        viewModelScope.launch {
            val day = requireDay(tripId, dayId) ?: run { _error.value = getDayNotLoadedMessage(); return@launch }
            if (day.items.any { it.postId == post.id }) {
                _error.value = "This post is already in this day"
                return@launch
            }
            val newItem = TripItem(
                id = "",
                dayId = dayId,
                postId = post.id,
                placeId = null,
                order = day.items.size,
                notes = null
            )
            tripRepository.addItemToDay(tripId, dayId, newItem).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _itemAdded.value = true
                        loadDay(tripId, dayId)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to add post"
                    }
                }
            }
        }
    }

    // ─────────────────── Places ───────────────────

    fun searchPlaces(query: String) {
        if (query.length < 2) {
            _placeSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val suggestions = placeInfoRepository.searchLocationSuggestions(query).firstOrNull() ?: emptyList()
                _placeSuggestions.postValue(suggestions)
            } catch (_: Exception) {
                _placeSuggestions.postValue(emptyList())
            }
        }
    }

    fun addGooglePlaceToDay(tripId: String, dayId: String, googlePlaceId: String) {
        viewModelScope.launch {
            val day0 = requireDay(tripId, dayId) ?: run { _error.value = getDayNotLoadedMessage(); return@launch }
            if (day0.items.any { it.placeId == googlePlaceId }) {
                _error.value = "This place is already in this day"
                return@launch
            }
            _isPlaceLoading.value = true
            try {
                val place = placeInfoRepository.getPlaceInfoFromGooglePlaceId(googlePlaceId).first()
                _isPlaceLoading.value = false
                addPlaceToDay(tripId, dayId, place)
            } catch (e: Exception) {
                _isPlaceLoading.value = false
                _error.value = "Failed to load place: ${e.message}"
            }
        }
    }

    private suspend fun addPlaceToDay(tripId: String, dayId: String, place: PlaceInfo) {
        val day = requireDay(tripId, dayId) ?: run {
            _error.value = getDayNotLoadedMessage()
            return
        }
        val newItem = TripItem(
            id = "",
            dayId = dayId,
            postId = null,
            placeId = place.xid,
            order = day.items.size,
            notes = null,
            place = place
        )
        tripRepository.addItemToDay(tripId, dayId, newItem).collect { result ->
            when (result) {
                is Result.Loading -> _isLoading.value = true
                is Result.Success -> {
                    _isLoading.value = false
                    _itemAdded.value = true
                    loadDay(tripId, dayId)
                }
                is Result.Error -> {
                    _isLoading.value = false
                    _error.value = result.message ?: "Failed to add place"
                }
            }
        }
    }

    // ─────────────────── Remove item ───────────────────

    fun removeItemFromDay(tripId: String, dayId: String, itemId: String) {
        viewModelScope.launch {
            val day = requireDay(tripId, dayId) ?: run { _error.value = getDayNotLoadedMessage(); return@launch }
            val trip = _currentTrip.value ?: run { _error.value = "Trip not loaded"; return@launch }
            val updatedItems = day.items.filter { it.id != itemId }
                .mapIndexed { index, item -> item.copy(order = index) }
            val updatedDay = day.copy(items = updatedItems)
            val updatedTrip = trip.copy(
                days = trip.days.map { if (it.id == dayId) updatedDay else it }
            )
            tripRepository.updateTrip(updatedTrip).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                        _currentDay.value = updatedDay
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to remove item"
                    }
                }
            }
        }
    }

    // ─────────────────── Save day (persist date) ───────────────────

    fun saveDay(tripId: String, dayId: String, dateMillis: Long?, description: String?) {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId).firstOrNull() ?: run {
                _error.value = "Trip not found"
                return@launch
            }
            val updatedTrip = trip.copy(
                days = trip.days.map { day ->
                    if (day.id == dayId) day.copy(
                        date = dateMillis,
                        description = description?.trim()?.takeIf { it.isNotBlank() }
                    ) else day
                }
            )
            tripRepository.updateTrip(updatedTrip).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _currentTrip.value = result.data
                        _currentDay.value = result.data.days.find { it.id == dayId }
                        _daySaved.value = true
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to save day"
                    }
                }
            }
        }
    }

    // ─────────────────── Reorder items ───────────────────

    fun reorderItems(dayId: String, reorderedItems: List<TripItem>) {
        viewModelScope.launch {
            tripRepository.reorderItems(dayId, reorderedItems).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        val tripId = _currentTrip.value?.id ?: return@collect
                        loadDay(tripId, dayId)
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Failed to reorder items"
                    }
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearItemAdded() {
        _itemAdded.value = false
    }

    fun clearDaySaved() {
        _daySaved.value = false
    }

    fun clearPlaceSuggestions() {
        _placeSuggestions.value = emptyList()
    }
}
