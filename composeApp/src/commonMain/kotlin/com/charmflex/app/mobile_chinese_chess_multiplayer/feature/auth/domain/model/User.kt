package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val token: String,
    val name: String,
    val email: String?,
    val isGuest: Boolean
)