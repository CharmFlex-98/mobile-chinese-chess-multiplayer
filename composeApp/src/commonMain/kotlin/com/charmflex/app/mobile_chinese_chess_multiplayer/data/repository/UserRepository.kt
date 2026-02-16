package com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.local.AuthLocalStorage
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.local.GameLocalDataSource
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.ApiClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.SupabaseAuthClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.PlayerDto
import com.charmflex.app.mobile_chinese_chess_multiplayer.domain.model.AuthType
import com.charmflex.app.mobile_chinese_chess_multiplayer.domain.model.AuthUser
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserRepository(
    private val apiClient: ApiClient,
    private val webSocketClient: WebSocketClient,
    private val localDataSource: GameLocalDataSource,
    private val supabaseAuthClient: SupabaseAuthClient,
    private val authLocalStorage: AuthLocalStorage
) {
    val currentPlayer: StateFlow<PlayerDto?> = localDataSource.currentPlayer

    private val _authUser = MutableStateFlow<AuthUser?>(null)
    val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

    val isGuest: Boolean get() = _authUser.value?.isGuest == true

    val sessionStatus get() = supabaseAuthClient.sessionStatus

    suspend fun signInWithGoogle() {
        supabaseAuthClient.signInWithGoogle()
    }

    suspend fun handleOAuthSession(): Result<AuthUser> {
        return try {
            val accessToken = supabaseAuthClient.currentAccessToken()
                ?: return Result.failure(Exception("No session after sign-in"))
            val userId = supabaseAuthClient.currentUserId()
                ?: return Result.failure(Exception("No user in session"))
            val displayName = supabaseAuthClient.currentDisplayName() ?: "Player"
            val email = supabaseAuthClient.currentUserEmail()

            val authUser = AuthUser(
                id = userId,
                displayName = displayName,
                email = email,
                authType = AuthType.SUPABASE,
                accessToken = accessToken
            )
            authLocalStorage.saveSession(authUser)
            _authUser.value = authUser

            registerWithGameServer(authUser)

            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun registerWithGameServer(authUser: AuthUser) {
        try {
            val response = apiClient.loginWithSupabase(authUser.accessToken, authUser.displayName)
            localDataSource.saveAuthToken(response.token)
            localDataSource.savePlayer(response.player)
            apiClient.setAuthToken(response.token)
            webSocketClient.setAuthToken(response.token)
        } catch (e: Exception) {
            println("[USER] Game server registration failed: ${e.message}")
        }
    }

    suspend fun loginAsGuest(name: String): Result<PlayerDto> {
        println("[USER] Login as guest: $name")
        return try {
            val response = apiClient.loginAsGuest(name)
            println("[USER] Guest login OK: id=${response.player.id.take(8)}... name=${response.player.name}")
            localDataSource.saveAuthToken(response.token)
            localDataSource.savePlayer(response.player)
            apiClient.setAuthToken(response.token)
            webSocketClient.setAuthToken(response.token)

            val authUser = AuthUser(
                id = response.player.id,
                displayName = response.player.name,
                authType = AuthType.GUEST,
                accessToken = response.token
            )
            authLocalStorage.saveSession(authUser)
            _authUser.value = authUser

            Result.success(response.player)
        } catch (e: Exception) {
            println("[USER] Guest login FAILED: ${e::class.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun restoreSession(): Boolean {
        val savedUser = authLocalStorage.getSession() ?: return false

        if (savedUser.authType == AuthType.SUPABASE) {
            // supabase-kt auto-restores session from storage on init.
            // Wait for it to finish loading, then check if authenticated.
            val status = supabaseAuthClient.sessionStatus.first { it !is SessionStatus.Initializing }
            if (status is SessionStatus.Authenticated) {
                val accessToken = supabaseAuthClient.currentAccessToken() ?: run {
                    authLocalStorage.clear()
                    return false
                }
                val updatedUser = savedUser.copy(accessToken = accessToken)
                authLocalStorage.saveSession(updatedUser)
                _authUser.value = updatedUser
                registerWithGameServer(updatedUser)
                return true
            }
            authLocalStorage.clear()
            return false
        } else {
            // Guest - re-login with a new guest session
            _authUser.value = savedUser
            loginAsGuest(savedUser.displayName)
            return true
        }
    }

    fun isLoggedIn(): Boolean = localDataSource.getAuthToken() != null

    fun logout() {
        val user = _authUser.value
        if (user?.authType == AuthType.SUPABASE) {
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    supabaseAuthClient.signOut()
                } catch (e: Exception) {
                    println("[USER] Sign out failed: ${e.message}")
                }
            }
        }
        _authUser.value = null
        authLocalStorage.clear()
        localDataSource.clear()
        webSocketClient.disconnect()
    }

    fun getCurrentPlayer(): PlayerDto? = localDataSource.getCurrentPlayer()
}
