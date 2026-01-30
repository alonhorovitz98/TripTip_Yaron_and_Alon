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
import com.example.triptip_yaron_and_alon.data.repository.UserRepository
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authDataSource = FirebaseAuthDataSource()
    private val firestoreDataSource = FirestoreDataSource()
    private val storageDataSource = FirebaseStorageDataSource()
    private val database = TripTipDatabase.getDatabase(application)
    private val userRepository = UserRepository(
        database.userDao(),
        authDataSource,
        firestoreDataSource,
        storageDataSource
    )
    private val authRepository = AuthRepository(
        authDataSource,
        firestoreDataSource,
        storageDataSource,
        database.userDao()
    )
    
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
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getCurrentUser().collect { user ->
                _user.value = user
                _isLoading.value = false
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
