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
import com.example.triptip_yaron_and_alon.data.repository.PlaceInfoRepository
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.WeatherInfo
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
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
    
    // Current post
    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post
    
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
    
    // Current user ID (simplified - should come from auth)
    private var currentUserId: String? = null
    
    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            postRepository.getPostById(postId).collect { post ->
                _post.value = post
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            postRepository.getUserPosts(userId).collect { posts ->
                _userPosts.value = posts
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun createPost(text: String, imageUri: Uri?, location: String?, latitude: Double?, longitude: Double?) {
        if (text.isBlank()) {
            _error.value = "Post text cannot be empty"
            return
        }
        
        viewModelScope.launch {
            // Get current user ID
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id
                ?: run {
                    _error.value = "User not authenticated"
                    return@launch
                }
            
            // Create Post object
            val post = Post(
                id = "", // Will be generated by Firestore
                userId = userId,
                userName = "", // Will be loaded from user data
                userImageUrl = null,
                text = text,
                imageUrl = null, // Will be set after image upload
                createdAt = System.currentTimeMillis(),
                location = location,
                latitude = latitude,
                longitude = longitude,
                placeXid = null
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
}
