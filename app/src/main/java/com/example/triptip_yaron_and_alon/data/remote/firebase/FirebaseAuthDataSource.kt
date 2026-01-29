package com.example.triptip_yaron_and_alon.data.remote.firebase

import com.example.triptip_yaron_and_alon.domain.model.User
import com.example.triptip_yaron_and_alon.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Data source for Firebase Authentication operations.
 * All methods are asynchronous and use coroutines.
 */
class FirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    /**
     * Sign up a new user with email and password.
     * Returns Flow<Result<User>> with the created user.
     */
    fun signUp(email: String, password: String): Flow<Result<User>> = flow {
        emit(Result.Loading)
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user?.toDomain() 
                ?: throw IllegalStateException("User creation succeeded but user is null")
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }
    
    /**
     * Sign in an existing user with email and password.
     * Returns Flow<Result<User>> with the signed-in user.
     */
    fun signIn(email: String, password: String): Flow<Result<User>> = flow {
        emit(Result.Loading)
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user?.toDomain()
                ?: throw IllegalStateException("Sign in succeeded but user is null")
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }
    
    /**
     * Sign out the current user.
     * Returns Result<Unit> indicating success or failure.
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
    
    /**
     * Get the current authenticated user.
     * Returns Flow<User?> that emits null if no user is signed in.
     */
    fun getCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.toDomain()
            trySend(user)
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        // Emit current user immediately
        val currentUser = firebaseAuth.currentUser?.toDomain()
        trySend(currentUser)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    /**
     * Update user profile (name and/or profile image).
     * Returns Flow<Result<User>> with the updated user.
     */
    fun updateProfile(name: String?, imageUri: String?): Flow<Result<User>> = flow {
        emit(Result.Loading)
        try {
            val user = firebaseAuth.currentUser
                ?: throw IllegalStateException("No user is currently signed in")
            
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            if (name != null) {
                profileUpdates.displayName = name
            }
            if (imageUri != null) {
                // Note: Firebase Auth doesn't directly support profile image URL updates
                // The imageUri should be uploaded to Storage first, then the URL passed here
                // For now, we'll handle this in the repository layer
            }
            
            user.updateProfile(profileUpdates.build()).await()
            user.reload().await()
            
            val updatedUser = firebaseAuth.currentUser?.toDomain()
                ?: throw IllegalStateException("Profile update succeeded but user is null")
            
            emit(Result.Success(updatedUser))
        } catch (e: Exception) {
            emit(Result.Error(e, e.message))
        }
    }
    
    /**
     * Check if a user is currently logged in.
     * Returns Flow<Boolean>.
     */
    fun isUserLoggedIn(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        // Emit current state immediately
        trySend(firebaseAuth.currentUser != null)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    /**
     * Extension function to convert FirebaseUser to domain User model.
     */
    private fun FirebaseUser.toDomain(): User {
        return User(
            id = uid,
            email = email ?: "",
            name = displayName ?: "",
            profileImageUrl = photoUrl?.toString()
        )
    }
}

