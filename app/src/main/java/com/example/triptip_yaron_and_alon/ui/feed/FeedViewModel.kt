package com.example.triptip_yaron_and_alon.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
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
    
    // Use lazy initialization to defer heavy object creation
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val storageDataSource by lazy { FirebaseStorageDataSource(application) }
    private val postRepository by lazy {
        PostRepository(
            database.postDao(),
            firestoreDataSource,
            storageDataSource
        )
    }
    
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
    
    // Removed init block - loadPosts() should be called explicitly from Fragment
    
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
            
            postRepository.getPostsPaginated(currentPage, pageSize).collect { newPosts ->
                if (newPosts.isEmpty()) {
                    isLastPage = true
                } else {
                    val currentPosts = _posts.value ?: emptyList()
                    _posts.value = currentPosts + newPosts
                }
                _isLoading.value = false
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
