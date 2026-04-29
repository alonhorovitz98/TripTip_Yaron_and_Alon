package com.example.triptip_yaron_and_alon.ui.post

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.NotificationsDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.CommentsDataSource
import com.example.triptip_yaron_and_alon.data.repository.CommentsRepository
import com.example.triptip_yaron_and_alon.data.repository.PlaceInfoRepository
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.data.repository.UserRepository
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.Comment
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.WeatherInfo
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {
    
    // Use lazy initialization to defer heavy object creation
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val storageDataSource by lazy { FirebaseStorageDataSource(application) }
    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val postRepository by lazy {
        PostRepository(
            database.postDao(),
            firestoreDataSource,
            storageDataSource
        )
    }
    private val placeInfoRepository by lazy { PlaceInfoRepository() }
    private val notificationsDataSource by lazy { NotificationsDataSource() }
    private val commentsDataSource by lazy { CommentsDataSource() }
    private val commentsRepository by lazy { CommentsRepository(commentsDataSource, database.commentDao()) }
    private val userRepository by lazy {
        UserRepository(
            database.userDao(),
            authDataSource,
            firestoreDataSource,
            storageDataSource
        )
    }

    // Current post
    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post
    
    // Whether current user liked the current post (for details screen like button)
    private val _isLikedByCurrentUser = MutableLiveData<Boolean>(false)
    val isLikedByCurrentUser: LiveData<Boolean> = _isLikedByCurrentUser
    
    // Comments for current post
    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments
    
    // User posts
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts
    
    // Operation result
    private val _operationResult = MutableLiveData<Result<Post>>()
    val operationResult: LiveData<Result<Post>> = _operationResult
    
    // Delete result
    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult
    
    // Loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // API Integration (Step 12.3)
    private val _weather = MutableLiveData<WeatherInfo?>()
    val weather: LiveData<WeatherInfo?> = _weather
    
    private val _weatherLoading = MutableLiveData<Boolean>()
    val weatherLoading: LiveData<Boolean> = _weatherLoading
    
    private val _weatherError = MutableLiveData<String?>()
    val weatherError: LiveData<String?> = _weatherError
    
    private val _nearbyPlaces = MutableLiveData<List<PlaceInfo>>()
    val nearbyPlaces: LiveData<List<PlaceInfo>> = _nearbyPlaces
    
    private val _placesLoading = MutableLiveData<Boolean>()
    val placesLoading: LiveData<Boolean> = _placesLoading
    
    private val _placesError = MutableLiveData<String?>()
    val placesError: LiveData<String?> = _placesError
    
    // Location suggestions for autocomplete
    private val _locationSuggestions = MutableLiveData<List<LocationSuggestion>>()
    val locationSuggestions: LiveData<List<LocationSuggestion>> = _locationSuggestions
    
    private val _locationSuggestionsLoading = MutableLiveData<Boolean>()
    val locationSuggestionsLoading: LiveData<Boolean> = _locationSuggestionsLoading
    private var locationSearchJob: Job? = null
    private var latestLocationQuery: String = ""
    
    // Current user ID (simplified - should come from auth)
    private var currentUserId: String? = null
    
    fun loadPost(postId: String) {
        viewModelScope.launch {
            currentUserId = authDataSource.getCurrentUser().firstOrNull()?.id
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            postRepository.getPostById(postId).collect { post ->
                _post.value = post
                _isLikedByCurrentUser.value = currentUserId?.let { post?.likedBy?.contains(it) == true } ?: false
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun likePost(postId: String) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUserSnapshot() ?: return@launch
            val uid = user.id
            when (val r = postRepository.likePost(postId, uid)) {
                is Result.Success -> {
                    val ownerId = r.data
                    if (ownerId != null && ownerId != uid) {
                        val label = user.name
                        notificationsDataSource.createNotification(
                            recipientUserId = ownerId,
                            type = NotificationsDataSource.TYPE_LIKE,
                            actorUserId = uid,
                            actorUserName = label,
                            targetPostId = postId,
                            message = "$label liked your post"
                        )
                    }
                }
                is Result.Error -> _error.value = r.message
                is Result.Loading -> { }
            }
        }
    }
    
    fun unlikePost(postId: String) {
        viewModelScope.launch {
            val uid = currentUserId ?: authDataSource.getCurrentUser().firstOrNull()?.id ?: return@launch
            when (val r = postRepository.unlikePost(postId, uid)) {
                is Result.Success -> { }
                is Result.Error -> _error.value = r.message
                is Result.Loading -> { }
            }
        }
    }
    
    fun toggleLike(postId: String) {
        val post = _post.value ?: return
        val uid = currentUserId ?: return
        if (post.likedBy.contains(uid)) unlikePost(postId) else likePost(postId)
    }
    
    fun loadComments(postId: String) {
        viewModelScope.launch {
            commentsRepository.getCommentsByPost(postId).collect { list ->
                _comments.postValue(list)
            }
        }
    }
    
    fun addComment(postId: String, text: String, imageUri: Uri? = null) {
        if (text.isBlank() && imageUri == null) return
        viewModelScope.launch {
            var imageUrl: String? = null
            if (imageUri != null) {
                when (val r = storageDataSource.uploadImageSync(imageUri, Constants.STORAGE_COMMENT_IMAGES)) {
                    is Result.Success -> imageUrl = r.data
                    is Result.Error -> {
                        _error.value = r.message
                        return@launch
                    }
                    is Result.Loading -> { }
                }
            }
            val user = userRepository.getCurrentUserSnapshot() ?: return@launch
            val displayName = user.name
            commentsRepository.addComment(
                postId,
                text.ifBlank { "" },
                imageUrl,
                null,
                userName = displayName,
                userAvatarUrl = user.profileImageUrl
            ).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val ownerId = _post.value?.userId
                        if (ownerId != null && ownerId != user.id) {
                            val label = user.name
                            notificationsDataSource.createNotification(
                                recipientUserId = ownerId,
                                type = NotificationsDataSource.TYPE_COMMENT,
                                actorUserId = user.id,
                                actorUserName = label,
                                targetPostId = postId,
                                message = "$label commented on your post"
                            )
                        }
                    }
                    is Result.Error -> _error.value = result.message
                    is Result.Loading -> { }
                }
            }
        }
    }
    
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            postRepository.getMyPosts(userId).collect { posts ->
                _userPosts.value = posts
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    /**
     * Search for location suggestions (for autocomplete).
     * Uses Google Places API for better quality results.
     */
    fun searchLocationSuggestions(query: String) {
        if (query.length < 2) {
            latestLocationQuery = ""
            locationSearchJob?.cancel()
            _locationSuggestions.value = emptyList()
            _locationSuggestionsLoading.value = false
            return
        }

        latestLocationQuery = query
        locationSearchJob?.cancel()
        locationSearchJob = viewModelScope.launch {
            _locationSuggestionsLoading.value = true
            placeInfoRepository.searchLocationSuggestions(query)
                .catch { e ->
                    _locationSuggestionsLoading.value = false
                    _error.value = "Failed to search locations: ${e.message}"
                }
                .collect { suggestions ->
                    // Ignore stale responses that return after a newer query was fired.
                    if (query == latestLocationQuery) {
                        _locationSuggestions.value = suggestions
                    }
                    _locationSuggestionsLoading.value = false
                }
        }
    }
    
    /**
     * Fetch place details from Google Places API by place_id.
     * Use this when user selects a Google Places suggestion to get coordinates.
     */
    fun fetchPlaceDetails(placeId: String) {
        viewModelScope.launch {
            _locationSuggestionsLoading.value = true
            placeInfoRepository.getGooglePlaceDetails(placeId)
                .catch { e ->
                    _locationSuggestionsLoading.value = false
                    _error.value = "Failed to fetch place details: ${e.message}"
                }
                .collect { suggestion ->
                    // Update the location suggestions with the fetched coordinates
                    val currentSuggestions = _locationSuggestions.value ?: emptyList()
                    val updatedSuggestions = currentSuggestions.map { 
                        if (it.googlePlaceId != null && it.googlePlaceId == placeId) {
                            suggestion // Replace with full details
                        } else {
                            it
                        }
                    }
                    _locationSuggestions.value = updatedSuggestions
                    _locationSuggestionsLoading.value = false
                }
        }
    }
    
    fun createPost(text: String, imageUri: Uri?, location: String?, latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val user = userRepository.getCurrentUserSnapshot() ?: run {
                _isLoading.value = false
                _error.value = "User not authenticated"
                return@launch
            }
            val userName = user.name
            val userImageUrl = user.profileImageUrl
            
            // Geocode location if name provided but no coordinates; also get price_level from Google when place_id present
            var finalLatitude = latitude
            var finalLongitude = longitude
            var priceLevel: Int? = null
            
            if (location != null && location.isNotBlank() && (latitude == null || longitude == null)) {
                try {
                    // Check if location contains a Google Places place_id (format: "place_name|place_id")
                    val parts = location.split("|")
                    if (parts.size == 2 && parts[1].isNotBlank()) {
                        // This is a Google Places place_id, fetch details (coords + price_level)
                        val placeDetails = placeInfoRepository.getGooglePlaceDetails(parts[1])
                            .firstOrNull()
                        val fullDetails = placeInfoRepository.getFullPlaceDetails(parts[1]).firstOrNull()
                        priceLevel = fullDetails?.priceLevel?.coerceIn(0, 4)
                        
                        if (placeDetails != null && placeDetails.latitude != 0.0 && placeDetails.longitude != 0.0) {
                            finalLatitude = placeDetails.latitude
                            finalLongitude = placeDetails.longitude
                        } else {
                            // Fallback to Nominatim geocoding
                            val coordinates = placeInfoRepository.geocodeLocation(parts[0])
                                .firstOrNull()
                            
                            if (coordinates != null) {
                                finalLatitude = coordinates.first
                                finalLongitude = coordinates.second
                            }
                        }
                    } else {
                        // Regular location name, use Nominatim geocoding
                        val coordinates = placeInfoRepository.geocodeLocation(location)
                            .firstOrNull()
                        
                        if (coordinates != null) {
                            finalLatitude = coordinates.first
                            finalLongitude = coordinates.second
                        } else {
                            // Location not found, but continue with location name only
                            _error.value = "Location not found, but post will be created with location name"
                        }
                    }
                } catch (e: Exception) {
                    // Geocoding failed, but continue with location name only
                    _error.value = "Could not get coordinates for location, but post will be created"
                }
            }
            
            // Create Post object with real user display name and avatar
            val post = Post(
                id = "", // Will be generated by Firestore
                userId = user.id,
                userName = userName,
                userImageUrl = userImageUrl,
                text = text,
                imageUrl = null, // Will be set after image upload
                createdAt = System.currentTimeMillis(),
                location = location,
                latitude = finalLatitude,
                longitude = finalLongitude,
                placeXid = null,
                priceLevel = priceLevel
            )
            
            postRepository.createPost(post, imageUri).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _operationResult.value = result
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
    
    fun updatePost(postId: String, text: String, imageUri: Uri?) {
        if (text.isBlank()) {
            _error.value = "Post text cannot be empty"
            return
        }
        
        viewModelScope.launch {
            // Get current post
            val currentPost = postRepository.getPostById(postId).firstOrNull()
                ?: run {
                    _error.value = "Post not found"
                    return@launch
                }
            
            // Create updated Post object
            val updatedPost = currentPost.copy(
                text = text
            )
            
            postRepository.updatePost(updatedPost, imageUri).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _operationResult.value = result
                        _post.value = result.data
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
    
    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId).collect { result ->
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
    
    // API Integration (Step 12.3)
    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _weatherLoading.value = true
            _weatherError.value = null
            placeInfoRepository.getWeather(lat, lon)
                .catch { e ->
                    _weatherLoading.value = false
                    _weatherError.value = "Failed to load weather: ${e.message}"
                }
                .collect { weatherInfo ->
                    _weather.value = weatherInfo
                    _weatherLoading.value = false
                }
        }
    }
    
    fun loadNearbyPlaces(lat: Double, lon: Double) {
        viewModelScope.launch {
            _placesLoading.value = true
            _placesError.value = null
            placeInfoRepository.getNearbyPlaces(lat, lon)
                .catch { e ->
                    _placesLoading.value = false
                    _placesError.value = "Failed to load places: ${e.message}"
                }
                .collect { places ->
                    _nearbyPlaces.value = places
                    _placesLoading.value = false
                }
        }
    }
    
    /**
     * Load weather for a location name (geocodes first, then loads weather).
     */
    fun loadWeatherForLocation(locationName: String) {
        viewModelScope.launch {
            _weatherLoading.value = true
            _weatherError.value = null
            
            try {
                val coordinates = placeInfoRepository.geocodeLocation(locationName)
                    .firstOrNull()
                
                if (coordinates != null) {
                    loadWeather(coordinates.first, coordinates.second)
                } else {
                    _weatherLoading.value = false
                    _weatherError.value = "Location not found"
                }
            } catch (e: Exception) {
                _weatherLoading.value = false
                _weatherError.value = "Failed to geocode location: ${e.message}"
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
}
