package com.example.triptip_yaron_and_alon.data.repository

import android.net.Uri
import com.example.triptip_yaron_and_alon.data.local.database.TripTipDatabase
import com.example.triptip_yaron_and_alon.data.local.database.dao.UserDao
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.domain.mapper.UserMapper
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for authentication operations.
 * Combines Firebase Auth with Room user caching.
 */
class AuthRepository(
    private val authDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: FirebaseStorageDataSource,
    private val userDao: UserDao
) {
    
    /**
     * Sign up a new user.
     * Creates user in Firebase Auth, saves to Firestore, and caches in Room.
     */
    fun signUp(email: String, password: String): Flow<Result<User>> = flow {
        authDataSource.signUp(email, password)
            .collect { result ->
                when (result) {
                    is Result.Success -> {
                        val user = result.data
                        // Save to Firestore
                        firestoreDataSource.saveUser(user)
                        // Cache in Room
                        userDao.insert(UserMapper.toEntity(user))
                        emit(Result.Success(user))
                    }
                    is Result.Error -> emit(result)
                    is Result.Loading -> emit(result)
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign in an existing user.
     * Authenticates with Firebase, loads user data, and caches in Room.
     */
    fun signIn(email: String, password: String): Flow<Result<User>> = flow {
        authDataSource.signIn(email, password)
            .collect { result ->
                when (result) {
                    is Result.Success -> {
                        val user = result.data
                        // Try to load full user data from Firestore
                        val finalUser = try {
                            withContext(Dispatchers.IO) {
                                firestoreDataSource.getUser(user.id)
                                    .catch { emit(null) }
                                    .firstOrNull() ?: user
                            }
                        } catch (e: Exception) {
                            // If Firestore fails, use Firebase Auth user
                            user
                        }
                        // Cache in Room
                        withContext(Dispatchers.IO) {
                            userDao.insert(UserMapper.toEntity(finalUser))
                        }
                        emit(Result.Success(finalUser))
                    }
                    is Result.Error -> emit(result)
                    is Result.Loading -> emit(result)
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign out the current user.
     * Signs out from Firebase and clears local cache.
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            val result = authDataSource.signOut()
            // Clear user cache (optional - you might want to keep it for offline access)
            // userDao.deleteAll()
            result
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Get the current authenticated user.
     * Checks Firebase first, then Room cache.
     */
    fun getCurrentUser(): Flow<User?> = flow {
        authDataSource.getCurrentUser()
            .collect { firebaseUser ->
                if (firebaseUser != null) {
                    // Try to get from Firestore for full profile
                    val user = try {
                        withContext(Dispatchers.IO) {
                            firestoreDataSource.getUser(firebaseUser.id)
                                .catch { emit(null) }
                                .firstOrNull() ?: firebaseUser
                        }
                    } catch (e: Exception) {
                        firebaseUser
                    }
                    // Cache in Room
                    withContext(Dispatchers.IO) {
                        userDao.insert(UserMapper.toEntity(user))
                    }
                    emit(user)
                } else {
                    // No Firebase user, try Room cache
                    val cachedUser = try {
                        withContext(Dispatchers.IO) {
                            userDao.getAllUsers()
                                .catch { emit(emptyList()) }
                                .firstOrNull()
                                ?.firstOrNull()
                                ?.let { UserMapper.toDomain(it) }
                        }
                    } catch (e: Exception) {
                        null
                    }
                    emit(cachedUser)
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update user profile (name and/or profile image).
     * Updates Firebase Auth, Firestore, and Room cache.
     */
    fun updateProfile(name: String?, imageUri: Uri?): Flow<Result<User>> = flow {
        emit(Result.Loading)
        
        try {
            var imageUrl: String? = null
            
            // Upload image if provided
            if (imageUri != null) {
                val uploadResult = storageDataSource.uploadImageSync(imageUri, Constants.STORAGE_PROFILE_IMAGES)
                
                when (uploadResult) {
                    is Result.Success -> {
                        imageUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        emit(uploadResult)
                        return@flow
                    }
                    is Result.Loading -> {
                        // Should not happen with sync function, but handle just in case
                        emit(Result.Error(Exception("Unexpected loading state"), "Upload failed"))
                        return@flow
                    }
                }
            }
            
            // Update Firebase Auth profile
            authDataSource.updateProfile(name, imageUrl)
                .collect { authResult ->
                    when (authResult) {
                        is Result.Success -> {
                            val user = authResult.data.copy(
                                name = name ?: authResult.data.name,
                                profileImageUrl = imageUrl ?: authResult.data.profileImageUrl
                            )
                            
                            // Update Firestore
                            firestoreDataSource.updateUser(user)
                            
                            // Update Room cache
                            userDao.insert(UserMapper.toEntity(user))
                            
                            emit(Result.Success(user))
                        }
                        is Result.Error -> emit(authResult)
                        is Result.Loading -> emit(authResult)
                    }
                }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if a user is currently logged in.
     * Returns Flow<Boolean>.
     */
    fun isUserLoggedIn(): Flow<Boolean> {
        return authDataSource.isUserLoggedIn()
    }
}

