package com.charmflex.app.mobile_chinese_chess_multiplayer.data.local

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.PlayerDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameLocalDataSource {
    private val _currentPlayer = MutableStateFlow<PlayerDto?>(null)
    val currentPlayer: StateFlow<PlayerDto?> = _currentPlayer.asStateFlow()

    private var authToken: String? = null

    fun savePlayer(player: PlayerDto) {
        _currentPlayer.value = player
    }

    fun saveAuthToken(token: String) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    fun getCurrentPlayer(): PlayerDto? = _currentPlayer.value

    fun clear() {
        _currentPlayer.value = null
        authToken = null
    }
}
