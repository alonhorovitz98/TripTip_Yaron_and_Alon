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
import com.example.triptip_yaron_and_alon.data.repository.TripsRepository
import com.example.triptip_yaron_and_alon.domain.model.DayItemType
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DayEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore by lazy { FirestoreDataSource() }
    private val storage by lazy { FirebaseStorageDataSource(application) }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val postRepository by lazy { PostRepository(database.postDao(), firestore, storage) }
    private val trips by lazy { TripsRepository(firestore, postRepository) }
    private val placeInfoRepository by lazy { PlaceInfoRepository() }

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

    private val _daySaved = MutableLiveData(false)
    val daySaved: LiveData<Boolean> = _daySaved

    private var postsJob: Job? = null

    fun loadDay(tripId: String, dayId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentDay.value = trips.getDayForEditor(tripId, dayId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load day"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAvailablePosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            postRepository.getPosts()
                .catch { e ->
                    _error.value = e.message ?: "Failed to load posts"
                }
                .collect { posts ->
                    _availablePosts.value = posts
                }
        }
    }

    fun addPostToDay(tripId: String, dayId: String, post: Post) {
        viewModelScope.launch {
            val day = trips.getDayForEditor(tripId, dayId) ?: run {
                _error.value = "Day not found"
                return@launch
            }
            if (day.items.any { it.isPost() && it.value == post.id }) {
                _error.value = "This post is already in this day"
                return@launch
            }
            _isLoading.value = true
            try {
                trips.addDayItem(tripId, dayId, DayItemType.POST, post.id)
                _currentDay.value = trips.getDayForEditor(tripId, dayId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add post"
            } finally {
                _isLoading.value = false
            }
        }
    }

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
            val day0 = trips.getDayForEditor(tripId, dayId) ?: run {
                _error.value = "Day not found"
                return@launch
            }
            _isPlaceLoading.value = true
            try {
                val place = placeInfoRepository.getPlaceInfoFromGooglePlaceId(googlePlaceId).first()
                _isPlaceLoading.value = false
                val name = place.name.trim()
                if (day0.items.any { it.isPlace() && it.value.equals(name, ignoreCase = true) }) {
                    _error.value = "This place is already in this day"
                    return@launch
                }
                _isLoading.value = true
                trips.addDayItem(tripId, dayId, DayItemType.PLACE, name)
                _currentDay.value = trips.getDayForEditor(tripId, dayId)
            } catch (e: Exception) {
                _isPlaceLoading.value = false
                _error.value = "Failed to load place: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeItemFromDay(tripId: String, dayId: String, itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                trips.deleteDayItem(tripId, dayId, itemId)
                _currentDay.value = trips.getDayForEditor(tripId, dayId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveDay(tripId: String, dayId: String, dateMillis: Long?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (dateMillis != null) {
                    trips.setDayDate(tripId, dayId, dateMillis)
                }
                _currentDay.value = trips.getDayForEditor(tripId, dayId)
                _daySaved.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save day"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearDaySaved() {
        _daySaved.value = false
    }

    fun clearPlaceSuggestions() {
        _placeSuggestions.value = emptyList()
    }
}
