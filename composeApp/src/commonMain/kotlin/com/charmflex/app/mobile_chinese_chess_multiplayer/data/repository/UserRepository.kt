package com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.local.GameLocalDataSource
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.ApiClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.PlayerDto
import kotlinx.coroutines.flow.StateFlow

class UserRepository(
    private val apiClient: ApiClient,
    private val webSocketClient: WebSocketClient,
    private val localDataSource: GameLocalDataSource
) {
    val currentPlayer: StateFlow<PlayerDto?> = localDataSource.currentPlayer

    suspend fun login(username: String, password: String): Result<PlayerDto> {
        return try {
            val response = apiClient.login(username, password)
            localDataSource.saveAuthToken(response.token)
            localDataSource.savePlayer(response.player)
            apiClient.setAuthToken(response.token)
            webSocketClient.setAuthToken(response.token)
            Result.success(response.player)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, password: String): Result<PlayerDto> {
        return try {
            val response = apiClient.register(username, password)
            localDataSource.saveAuthToken(response.token)
            localDataSource.savePlayer(response.player)
            apiClient.setAuthToken(response.token)
            webSocketClient.setAuthToken(response.token)
            Result.success(response.player)
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.success(response.player)
        } catch (e: Exception) {
            println("[USER] Guest login FAILED: ${e::class.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = localDataSource.getAuthToken() != null

    fun logout() {
        localDataSource.clear()
        webSocketClient.disconnect()
    }

    fun getCurrentPlayer(): PlayerDto? = localDataSource.getCurrentPlayer()
}
