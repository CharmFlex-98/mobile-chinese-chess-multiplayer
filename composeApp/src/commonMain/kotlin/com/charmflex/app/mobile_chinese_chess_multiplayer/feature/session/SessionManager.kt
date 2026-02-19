package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton
import kotlin.time.Clock

/**
 * A low level component that every else depend on this to get session state
 */
@Singleton
class SessionManager {
    private val _currentUserSession: MutableStateFlow<UserSession?> = MutableStateFlow(null)
    val currentUserSession = _currentUserSession.asStateFlow()

    fun onLogin(
        token: String,
        id: String,
        name: String,
        email: String?,
        isGuest: Boolean
    ) {
        _currentUserSession.value = UserSession(
            token = token,
            id = id,
            name = name,
            email = email,
            isGuest = isGuest,
            loginTime = Clock.System.now().toString()
        )
    }

    fun onLogout() {
        _currentUserSession.value = null
    }

    fun isLoggedIn() = _currentUserSession.value != null && _currentUserSession.value?.token?.isNotEmpty() == true
}

data class UserSession(
    val token: String,
    val id: String,
    val name: String,
    val email: String? = null,
    val isGuest: Boolean = false,
    val loginTime: String
)