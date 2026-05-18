package com.example.triptip_yaron_and_alon.ui.feed

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.NotificationsDataSource
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.data.repository.UserRepository
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for Feed screen
 */
class FeedViewModel(application: Application) : AndroidViewModel(application) {
    
    // Use lazy initialization to defer heavy object creation
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val storageDataSource by lazy { FirebaseStorageDataSource(application) }
    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val notificationsDataSource by lazy { NotificationsDataSource() }
    private val postRepository by lazy {
        PostRepository(
            database.postDao(),
            firestoreDataSource,
            storageDataSource
        )
    }
    private val userRepository by lazy {
        UserRepository(
            database.userDao(),
            authDataSource,
            firestoreDataSource,
            storageDataSource
        )
    }
    
    // LiveData for posts
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    // LiveData for current user's own posts
    private val _myPosts = MutableLiveData<List<Post>>()
    val myPosts: LiveData<List<Post>> = _myPosts

    // Current user id for like state and actions
    private val _currentUserId = MutableLiveData<String?>(null)
    val currentUserId: LiveData<String?> = _currentUserId
    
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
    
    // Job tracking to prevent multiple collectors
    private var loadPostsJob: Job? = null
    private var loadMyPostsJob: Job? = null

    // Removed init block - loadPosts() should be called explicitly from Fragment

    /**
     * Load current user's posts (cache-first, filtered).
     */
    fun loadMyPosts() {
        if (loadMyPostsJob?.isActive == true) return
        loadMyPostsJob = viewModelScope.launch {
            val userId = _currentUserId.value
                ?: authDataSource.getCurrentUser().firstOrNull()?.id
                ?: return@launch
            _currentUserId.value = userId
            var first = true
            postRepository.getMyPosts(userId).collect { list ->
                _myPosts.value = list
                if (first) { first = false }
            }
        }
    }

    /**
     * Load posts (cache-first strategy)
     * Prevents multiple collectors by tracking the job
     */
    fun loadPosts() {
        // Cancel existing job if active to prevent duplicate collectors
        if (loadPostsJob?.isActive == true) {
            return
        }
        
        loadPostsJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentUserId.value = authDataSource.getCurrentUser().firstOrNull()?.id
            
            var isFirstEmission = true
            postRepository.getPosts().collect { postsList ->
                _posts.value = postsList
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    /**
     * Like a post (real DB update). Refreshes feed via listener.
     */
    fun likePost(postId: String) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUserSnapshot() ?: return@launch
            val userId = user.id
            when (val r = postRepository.likePost(postId, userId)) {
                is com.example.triptip_yaron_and_alon.util.Result.Success -> {
                    val ownerId = r.data
                    if (ownerId != null && ownerId != userId) {
                        val label = user.name
                        when (
                            notificationsDataSource.createNotification(
                                recipientUserId = ownerId,
                                type = NotificationsDataSource.TYPE_LIKE,
                                actorUserId = userId,
                                actorUserName = label,
                                targetPostId = postId,
                                message = "$label liked your post"
                            )
                        ) {
                            is Result.Error -> Log.w("FeedViewModel", "like notification: ${it.message}")
                            else -> { }
                        }
                    }
                }
                is com.example.triptip_yaron_and_alon.util.Result.Error -> _error.value = r.message
                is com.example.triptip_yaron_and_alon.util.Result.Loading -> { }
            }
        }
    }
    
    /**
     * Unlike a post (real DB update).
     */
    fun unlikePost(postId: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: authDataSource.getCurrentUser().firstOrNull()?.id
            if (userId == null) return@launch
            when (val r = postRepository.unlikePost(postId, userId)) {
                is com.example.triptip_yaron_and_alon.util.Result.Success -> { }
                is com.example.triptip_yaron_and_alon.util.Result.Error -> _error.value = r.message
                is com.example.triptip_yaron_and_alon.util.Result.Loading -> { }
            }
        }
    }
    
    /**
     * Refresh posts (pull-to-refresh)
     */
    fun refreshPosts() {
        // Cancel existing job before refreshing
        loadPostsJob?.cancel()
        loadPostsJob = null
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
            _error.value = null
            currentPage++
            
            try {
                // Use first() instead of collect() for pagination - we only want one emission
                val newPosts = postRepository.getPostsPaginated(currentPage, pageSize).first()
                if (newPosts.isEmpty()) {
                    isLastPage = true
                } else {
                    val currentPosts = _posts.value ?: emptyList()
                    _posts.value = currentPosts + newPosts
                }
            } catch (e: Exception) {
                _error.value = "Failed to load more posts: ${e.message}"
                currentPage-- // Rollback page increment on error
            } finally {
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
