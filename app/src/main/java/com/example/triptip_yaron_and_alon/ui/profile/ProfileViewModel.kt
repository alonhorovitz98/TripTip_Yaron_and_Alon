package com.example.triptip_yaron_and_alon.ui.profile

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
import com.example.triptip_yaron_and_alon.data.repository.AuthRepository
import com.example.triptip_yaron_and_alon.data.repository.PostRepository
import com.example.triptip_yaron_and_alon.data.repository.UserRepository
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    // Use lazy initialization to defer heavy object creation
    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val storageDataSource by lazy { FirebaseStorageDataSource(application) }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val userRepository by lazy {
        UserRepository(
            database.userDao(),
            authDataSource,
            firestoreDataSource,
            storageDataSource
        )
    }
    private val authRepository by lazy {
        AuthRepository(
            authDataSource,
            firestoreDataSource,
            storageDataSource,
            database.userDao(),
            database.postDao()
        )
    }
    private val postRepository by lazy {
        PostRepository(
            database.postDao(),
            firestoreDataSource,
            storageDataSource
        )
    }
    
    // User
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user
    
    // Update result
    private val _updateResult = MutableLiveData<Result<User>>()
    val updateResult: LiveData<Result<User>> = _updateResult
    
    // Logout result
    private val _logoutResult = MutableLiveData<Result<Unit>>()
    val logoutResult: LiveData<Result<Unit>> = _logoutResult
    
    // Loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Job tracking to prevent multiple collectors
    private var loadProfileJob: Job? = null
    
    // Removed init block - loadProfile() should be called explicitly from Fragment
    
    fun loadProfile() {
        // Cancel existing job if active to prevent duplicate collectors
        if (loadProfileJob?.isActive == true) {
            return
        }
        
        loadProfileJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var isFirstEmission = true
            userRepository.getCurrentUser().collect { user ->
                _user.value = user
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }
            }
        }
    }
    
    fun updateProfile(name: String?, imageUri: Uri?) {
        if (name.isNullOrBlank() && imageUri == null) {
            _error.value = "Nothing to update"
            return
        }
        
        viewModelScope.launch {
            userRepository.updateProfile(name, imageUri).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _updateResult.value = result
                        _user.value = result.data
                        viewModelScope.launch(Dispatchers.IO) {
                            runCatching {
                                postRepository.propagateAuthorDisplayToPublishedContent(
                                    result.data.id,
                                    result.data.name,
                                    result.data.profileImageUrl
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                        _updateResult.value = result
                    }
                }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signOut()
            _isLoading.value = false
            _logoutResult.value = result
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
