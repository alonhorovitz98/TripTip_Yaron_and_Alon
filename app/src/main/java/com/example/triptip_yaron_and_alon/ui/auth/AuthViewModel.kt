package com.example.triptip_yaron_and_alon.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.data.repository.AuthRepository
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication operations
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    // Use lazy initialization to defer heavy object creation
    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val firestoreDataSource by lazy { FirestoreDataSource() }
    private val storageDataSource by lazy { FirebaseStorageDataSource(application) }
    private val database by lazy { TripTipDatabase.getDatabase(application) }
    private val authRepository by lazy {
        AuthRepository(
            authDataSource,
            firestoreDataSource,
            storageDataSource,
            database.userDao()
        )
    }
    
    // LiveData for login result
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult
    
    // LiveData for register result
    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult
    
    // LiveData for logout result
    private val _logoutResult = MutableLiveData<Result<Unit>>()
    val logoutResult: LiveData<Result<Unit>> = _logoutResult
    
    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData for error messages
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // LiveData for logged in state
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn
    
    // Removed init block - checkLoginStatus() should be called explicitly from Fragment
    
    /**
     * Check if user is currently logged in
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            authRepository.isUserLoggedIn().collect { loggedIn ->
                _isLoggedIn.value = loggedIn
            }
        }
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        // Validate input
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.value = "Invalid email format"
            return
        }
        
        viewModelScope.launch {
            authRepository.signIn(email, password).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _isLoading.value = true
                        _error.value = null
                    }
                    is Result.Success -> {
                        _isLoading.value = false
                        _loginResult.value = result
                        _isLoggedIn.value = true
                        _error.value = null
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Login failed"
                        _loginResult.value = result
                    }
                }
            }
        }
    }
    
    /**
     * Register a new user
     */
    fun register(email: String, password: String, name: String) {
        // Validate input
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _error.value = "All fields are required"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.value = "Invalid email format"
            return
        }
        
        if (password.length < 6) {
            _error.value = "Password must be at least 6 characters"
            return
        }
        
        viewModelScope.launch {
            authRepository.signUp(email, password).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _isLoading.value = true
                        _error.value = null
                    }
                    is Result.Success -> {
                        _isLoading.value = false
                        // Update user name after signup
                        val updatedUser = result.data.copy(name = name)
                        _registerResult.value = Result.Success(updatedUser)
                        _isLoggedIn.value = true
                        _error.value = null
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _error.value = result.message ?: "Registration failed"
                        _registerResult.value = result
                    }
                }
            }
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signOut()
            _isLoading.value = false
            _logoutResult.value = result
            _isLoggedIn.value = false
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
