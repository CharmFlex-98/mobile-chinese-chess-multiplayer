package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.data.SupabaseAuthClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.AuthRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import io.ktor.client.plugins.observer.ResponseObserver
import org.koin.core.annotation.Factory

@Factory
class AuthService(
    private val client: SupabaseAuthClient,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) {
    suspend fun exchangeCodeForSession(code: String) {
        client.exchangeCodeForSession(code)
    }

    suspend fun handleUserAuthenticated(): Result<Unit> {
        val res = authRepository.handleUserAuthenticated()
        if (res.isSuccess) {
            val user = res.getOrNull()
            if (user == null) return Result.failure(Exception("User is null"))

            sessionManager.onLogin(
                token = user.token,
                id = user.id,
                name = user.name,
                email = user.email,
                isGuest = user.isGuest
            )
            return Result.success(Unit)
        }

        return Result.failure(res.exceptionOrNull() ?: Exception("Unknown error"))
    }

    suspend fun restoreSession(): Result<Unit> {
        val res = authRepository.restoreSession()
        if (res.isSuccess) {
            val user = res.getOrNull()
            if (user == null) {
                return Result.failure(Exception("User is null"))
            }

            sessionManager.onLogin(
                name = user.name,
                id =  user.id,
                token = user.token,
                email = user.email,
                isGuest = user.isGuest
            )

            return Result.success(Unit)
        }

        return Result.failure(res.exceptionOrNull() ?: Exception("Unknown error"))
    }


    fun logout() {
        val isGuest = sessionManager.currentUserSession.value?.isGuest == true
        authRepository.logout(isGuest)
        sessionManager.onLogout()
    }
}