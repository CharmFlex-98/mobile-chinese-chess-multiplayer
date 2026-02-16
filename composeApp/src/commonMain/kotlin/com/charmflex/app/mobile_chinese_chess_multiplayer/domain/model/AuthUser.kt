package com.charmflex.app.mobile_chinese_chess_multiplayer.domain.model

data class AuthUser(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val authType: AuthType,
    val accessToken: String
) {
    val isGuest: Boolean get() = authType == AuthType.GUEST
}

enum class AuthType {
    SUPABASE,
    GUEST
}
