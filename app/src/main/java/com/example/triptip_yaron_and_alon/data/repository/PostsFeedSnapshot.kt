package com.example.triptip_yaron_and_alon.data.repository

import com.example.triptip_yaron_and_alon.domain.model.Post

/**
 * One emission from the shared posts feed: remote list plus optional sync failure
 * (e.g. wrong Firebase project, rules, or offline) so the UI can warn the user.
 */
data class PostsFeedSnapshot(
    val posts: List<Post>,
    val syncError: String? = null
)
