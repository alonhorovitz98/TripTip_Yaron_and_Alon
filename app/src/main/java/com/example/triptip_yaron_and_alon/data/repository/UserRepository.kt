package com.example.triptip_yaron_and_alon.data.repository

import android.net.Uri
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for user operations.
 * Combines Firebase Auth, Firestore, and Room caching.
 */
class UserRepository(
    private val userDao: UserDao,
    private val authDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: FirebaseStorageDataSource
) {
    
    /**
     * Get current user.
     * Checks Room cache first, then Firebase.
     */
    fun getCurrentUser(): Flow<User?> = flow {
        // Emit cached user immediately
        userDao.getAllUsers()
            .map { users -> users.firstOrNull()?.let { UserMapper.toDomain(it) } }
            .catch { emit(null) }
            .collect { cachedUser ->
                emit(cachedUser)
            }
        
        // Get from Firebase and update cache
        authDataSource.getCurrentUser()
            .catch { /* Ignore errors, use cache */ }
            .collect { firebaseUser ->
                if (firebaseUser != null) {
                    // Try to get full profile from Firestore
                    firestoreDataSource.getUser(firebaseUser.id)
                        .catch { emit(null) }
                        .collect { firestoreUser ->
                            val user = firestoreUser ?: firebaseUser
                            // Update cache
                            withContext(Dispatchers.IO) {
                                userDao.insert(UserMapper.toEntity(user))
                            }
                        }
                }
            }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update user profile.
     * Updates Firebase Auth, Firestore, and Room cache.
     */
    fun updateProfile(name: String?, imageUri: Uri?): Flow<Result<User>> = flow {
        emit(Result.Loading)
        
        try {
            var imageUrl: String? = null
            
            // Upload image if provided
            if (imageUri != null) {
                val uploadResult = storageDataSource.uploadImage(imageUri, Constants.STORAGE_PROFILE_IMAGES)
                    .first() // Get first emission instead of collecting
                
                when (uploadResult) {
                    is Result.Success -> {
                        imageUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        emit(uploadResult)
                        return@flow
                    }
                    is Result.Loading -> {
                        // Should not happen with first(), but handle just in case
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
                            withContext(Dispatchers.IO) {
                                userDao.insert(UserMapper.toEntity(user))
                            }
                            
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
}

