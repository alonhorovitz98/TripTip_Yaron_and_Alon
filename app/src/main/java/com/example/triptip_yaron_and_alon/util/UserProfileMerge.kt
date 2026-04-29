package com.example.triptip_yaron_and_alon.util

import com.example.triptip_yaron_and_alon.domain.model.User

/**
 * Combines Firestore `users/{id}` (canonical name + image URL) with Auth-backed [User]
 * so we never show empty display names when one source is missing.
 */
object UserProfileMerge {
    fun merge(firestore: User?, auth: User): User {
        val name = firestore?.name?.trim().orEmpty()
            .takeIf { it.isNotEmpty() }
            ?: auth.name.trim().takeIf { it.isNotEmpty() }
            ?: auth.email.substringBefore("@").trim()
                .takeIf { it.isNotEmpty() }
            ?: "Traveler"
        val email = when {
            !auth.email.isBlank() -> auth.email
            !firestore?.email.isNullOrBlank() -> firestore!!.email
            else -> auth.email
        }
        val photo = listOf(
            firestore?.profileImageUrl,
            auth.profileImageUrl
        )
            .map { it?.trim() }
            .firstOrNull { !it.isNullOrEmpty() }
        return User(
            id = auth.id,
            email = email,
            name = name,
            profileImageUrl = photo
        )
    }
}
