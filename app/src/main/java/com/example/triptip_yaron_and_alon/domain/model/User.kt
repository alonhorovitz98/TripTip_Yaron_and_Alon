package com.example.triptip_yaron_and_alon.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null
)

