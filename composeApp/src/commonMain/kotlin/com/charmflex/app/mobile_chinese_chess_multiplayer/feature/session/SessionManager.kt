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

    private val _pendingLevelUp: MutableStateFlow<Int?> = MutableStateFlow(null)
    val pendingLevelUp = _pendingLevelUp.asStateFlow()

    fun onLogin(
        token: String,
        id: String,
        name: String,
        email: String?,
        isGuest: Boolean,
        xp: Int = 0,
        level: Int = 1,
        avatarUrl: String? = null,
        isAdmin: Boolean = false
    ) {
        _currentUserSession.value = UserSession(
            token = token,
            id = id,
            name = name,
            email = email,
            isGuest = isGuest,
            loginTime = Clock.System.now().toString(),
            xp = xp,
            level = level,
            avatarUrl = avatarUrl,
            isAdmin = isAdmin
        )
    }

    fun updateXp(newXp: Int, newLevel: Int) {
        _currentUserSession.value = _currentUserSession.value?.copy(xp = newXp, level = newLevel)
    }

    fun setPendingLevelUp(newLevel: Int) {
        _pendingLevelUp.value = newLevel
    }

    fun clearPendingLevelUp() {
        _pendingLevelUp.value = null
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
    val loginTime: String,
    val xp: Int = 0,
    val level: Int = 1,
    val avatarUrl: String? = null,
    val isAdmin: Boolean = false
)