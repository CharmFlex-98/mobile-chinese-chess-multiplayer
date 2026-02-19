package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.model.User
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface AuthRepository {
    val sessionStatus: Flow<SessionStatus>
    suspend fun signIn()
    suspend fun signInAsGuest(userId: String, displayName: String, token: String): RegisterServerResponse
    suspend fun handleUserAuthenticated(): Result<User>
    suspend fun registerWithGameServer(registerServerRequest: RegisterServerRequest): RegisterServerResponse
    suspend fun restoreSession(): Result<User?>
    fun logout(isGuest: Boolean)
}


@Serializable
data class RegisterServerRequest(
    val uid: String,
    val displayName: String,
    val token: String
)

@Serializable
data class PlayerDto(
    val id: String,
    val name: String,
    val rating: Int = 1200
)

@Serializable
data class RegisterServerResponse(
    val uid: String,
    val token: String,
    val displayName: String,
    val guest: Boolean
)