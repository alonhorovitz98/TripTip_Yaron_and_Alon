package com.example.triptip_yaron_and_alon.ui.feed

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
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.launch

/**
 * ViewModel for Feed screen
 */
class FeedViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firestoreDataSource = FirestoreDataSource()
    private val storageDataSource = FirebaseStorageDataSource()
    private val authDataSource = FirebaseAuthDataSource()
    private val database = TripTipDatabase.getDatabase(application)
    private val postRepository = PostRepository(
        firestoreDataSource,
        storageDataSource,
        database.postDao()
    )
    
    // LiveData for posts
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Pagination
    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false
    
    init {
        loadPosts()
    }
    
    /**
     * Load posts (cache-first strategy)
     */
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            postRepository.getPosts().collect { postsList ->
                _posts.value = postsList
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh posts (pull-to-refresh)
     */
    fun refreshPosts() {
        currentPage = 0
        isLastPage = false
        loadPosts()
    }
    
    /**
     * Load more posts (lazy loading)
     */
    fun loadMorePosts() {
        if (isLastPage || _isLoading.value == true) return
        
        viewModelScope.launch {
            _isLoading.value = true
            currentPage++
            
            postRepository.getPostsPaginated(currentPage, pageSize).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val newPosts = result.data
                        if (newPosts.isEmpty()) {
                            isLastPage = true
                        } else {
                            val currentPosts = _posts.value ?: emptyList()
                            _posts.value = currentPosts + newPosts
                        }
                        _isLoading.value = false
                    }
                    is Result.Error -> {
                        _error.value = result.message
                        _isLoading.value = false
                    }
                    is Result.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}
