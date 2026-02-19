package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.domain.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.model.User
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.profile.Player
import kotlinx.coroutines.flow.StateFlow

interface HomeRepository {
    val currentPlayerData: StateFlow<PlayerData?>
}

data class PlayerData(
    val user: User,
    val player: Player
)