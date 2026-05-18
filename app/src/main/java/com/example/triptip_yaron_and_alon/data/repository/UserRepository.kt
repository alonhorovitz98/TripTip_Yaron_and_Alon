package com.example.triptip_yaron_and_alon.data.repository

import android.net.Uri
import com.example.triptip_yaron_and_alon.data.local.database.dao.UserDao
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseStorageDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirestoreDataSource
import com.example.triptip_yaron_and_alon.domain.mapper.UserMapper
import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Constants
import com.example.triptip_yaron_and_alon.util.UserProfileMerge
import com.example.triptip_yaron_and_alon.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Repository for user operations.
 * Combines Firebase Auth, Firestore, and Room caching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserRepository(
    private val userDao: UserDao,
    private val authDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: FirebaseStorageDataSource
) {
    
    /**
     * Get current user, merging Firestore `users/{id}` with Auth on every update.
     * Uses [flatMapLatest] so sign-out (auth emits null) cancels the Firestore listener and
     * does not block on a nested infinite [collect].
     */
    fun getCurrentUser(): Flow<User?> = userDao.getAllUsers()
        .map { users -> users.firstOrNull()?.let { UserMapper.toDomain(it) } }
        .catch { emit(null) }
        .flowOn(Dispatchers.IO)
        .flatMapLatest {
            authDataSource.getCurrentUser()
                .catch { }
                .flatMapLatest { firebaseUser ->
                    if (firebaseUser == null) {
                        flowOf(null)
                    } else {
                        firestoreDataSource.getUser(firebaseUser.id)
                            .catch { emit(null) }
                            .map { firestoreUser ->
                                UserProfileMerge.merge(firestoreUser, firebaseUser)
                            }
                            .onEach { merged ->
                                withContext(Dispatchers.IO) {
                                    userDao.insert(UserMapper.toEntity(merged))
                                }
                            }
                    }
                }
        }

    /**
     * Resolves the signed-in user once (Firestore + Auth + Room), for actions that need
     * correct [User.name] / [User.profileImageUrl] in the same call (e.g. comments, likes).
     */
    suspend fun getCurrentUserSnapshot(): User? = withContext(Dispatchers.IO) {
        val authUser = try {
            authDataSource.getCurrentUser().first { it != null }
        } catch (_: Exception) {
            null
        } ?: return@withContext null
        val firestoreUser = try {
            firestoreDataSource.getUser(authUser.id)
                .catch { emit(null) }
                .first()
        } catch (_: Exception) {
            null
        }
        val merged = UserProfileMerge.merge(firestoreUser, authUser)
        userDao.insert(UserMapper.toEntity(merged))
        merged
    }
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

