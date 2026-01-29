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
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firestoreDataSource = FirestoreDataSource()
    private val storageDataSource = FirebaseStorageDataSource()
    private val authDataSource = FirebaseAuthDataSource()
    private val database = TripTipDatabase.getDatabase(application)
    private val postRepository = PostRepository(
        firestoreDataSource,
        storageDataSource,
        database.postDao()
    )
    
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
    
    // API Integration (Step 12.3) - Placeholder for Alon's API Repository
    // TODO: Uncomment when Alon completes API Repository
    /*
    private val _weather = MutableLiveData<WeatherInfo?>()
    val weather: LiveData<WeatherInfo?> = _weather
    
    private val _nearbyPlaces = MutableLiveData<List<PlaceInfo>>()
    val nearbyPlaces: LiveData<List<PlaceInfo>> = _nearbyPlaces
    */
    
    // Current user ID (simplified - should come from auth)
    private var currentUserId: String? = null
    
    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            postRepository.getPostById(postId).collect { post ->
                _post.value = post
                _isLoading.value = false
            }
        }
    }
    
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            postRepository.getUserPosts(userId).collect { posts ->
                _userPosts.value = posts
                _isLoading.value = false
            }
        }
    }
    
    fun createPost(text: String, imageUri: Uri?, location: String?, latitude: Double?, longitude: Double?) {
        if (text.isBlank()) {
            _error.value = "Post text cannot be empty"
            return
        }
        
        viewModelScope.launch {
            postRepository.createPost(
                text = text,
                imageUri = imageUri,
                location = location,
                latitude = latitude,
                longitude = longitude
            ).collect { result ->
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
            postRepository.updatePost(postId, text, imageUri).collect { result ->
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
    
    // API Integration (Step 12.3) - Placeholder methods
    // TODO: Uncomment and implement when Alon completes API Repository
    /*
    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            // apiRepository.getWeather(lat, lon).collect { result ->
            //     when (result) {
            //         is Result.Success -> _weather.value = result.data
            //         is Result.Error -> _error.value = result.message
            //         else -> {}
            //     }
            // }
        }
    }
    
    fun loadNearbyPlaces(lat: Double, lon: Double) {
        viewModelScope.launch {
            // apiRepository.getNearbyPlaces(lat, lon).collect { result ->
            //     when (result) {
            //         is Result.Success -> _nearbyPlaces.value = result.data
            //         is Result.Error -> _error.value = result.message
            //         else -> {}
            //     }
            // }
        }
    }
    */
}
