package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.data.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.NetworkAttributes
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.NetworkClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.usePost
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.data.SupabaseAuthClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.model.User
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.AuthRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.RegisterServerRequest
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.RegisterServerResponse
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.storage.AuthLocalStorage
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.route.HomeRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton

@Singleton
class AuthRepositoryImpl(
    private val networkClient: NetworkClient,
    private val webSocketClient: WebSocketClient,
    private val supabaseAuthClient: SupabaseAuthClient,
    private val authLocalStorage: AuthLocalStorage,
    private val routeNavigator: RouteNavigator,
) : AuthRepository {
    override val sessionStatus get() = supabaseAuthClient.sessionStatus

    override suspend fun signIn() {
        supabaseAuthClient.signInWithGoogle()
    }

    override suspend fun handleUserAuthenticated(): Result<User> {
        return try {
            val accessToken = supabaseAuthClient.currentAccessToken()
                ?: return Result.failure(Exception("No session after sign-in"))
            val userId = supabaseAuthClient.currentUserId()
                ?: return Result.failure(Exception("No user in session"))
            val displayName = supabaseAuthClient.currentDisplayName() ?: "Player"
            val email = supabaseAuthClient.currentUserEmail()

            val authUser = User(
                id = userId,
                token = accessToken,
                name = displayName,
                email = email,
                isGuest = false,
            )
            authLocalStorage.saveSession(authUser)
            registerWithGameServer(RegisterServerRequest(uid = authUser.id, displayName = authUser.name, token = authUser.token))

            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerWithGameServer(registerServerRequest: RegisterServerRequest): RegisterServerResponse {
        val response: RegisterServerResponse = networkClient.usePost(
            endPoint = "/api/auth/login/verify",
            body = registerServerRequest
        ) {
            add(NetworkAttributes.needToken)
        }
        return response
    }

    override suspend fun signInAsGuest(userId: String, displayName: String, token: String): RegisterServerResponse {
        val response: RegisterServerResponse = networkClient.usePost(
            endPoint = "/api/auth/guest",
            body = RegisterServerRequest(userId, displayName, token)
        )

        return response
    }

    override suspend fun restoreSession(): Result<User?> {
        try {
            val savedUser = authLocalStorage.getSession() ?: return Result.success(null)

            if (!savedUser.isGuest) {
                // supabase-kt auto-restores session from storage on init.
                // Wait for it to finish loading, then check if authenticated.
                val status = supabaseAuthClient.sessionStatus.first { it !is SessionStatus.Initializing }
                if (status is SessionStatus.Authenticated) {
                    val accessToken = supabaseAuthClient.currentAccessToken() ?: run {
                        authLocalStorage.clear()
                        return Result.failure(Exception("Cannot obtain currentAccessToken from server"))
                    }

                    val updatedUser = savedUser.copy(token = accessToken)
                    authLocalStorage.saveSession(updatedUser)
                    registerWithGameServer(RegisterServerRequest(uid = updatedUser.id, displayName = updatedUser.name, token = updatedUser.token))
                    routeNavigator.navigateTo(HomeRoute.ROOT)
                    return Result.success(updatedUser)
                }
                authLocalStorage.clear()
                return Result.success(null)
            } else {
                // Guest - re-login with a new guest session
                signInAsGuest(userId = savedUser.id, displayName = savedUser.name, token = savedUser.token)
                return Result.success(savedUser)
            }
        } catch (e : Exception) {
            return Result.failure(e)
        }
    }


    override fun logout(isGuest: Boolean) {
        if (!isGuest) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    supabaseAuthClient.signOut()
                } catch (e: Exception) {
                    println("[USER] Sign out failed: ${e.message}")
                }
            }
        }
        authLocalStorage.clear()
        webSocketClient.disconnect()
    }
}