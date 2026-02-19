package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeRepositoryImpl : HomeRepository {
    private val _currentPlayerData: MutableStateFlow<PlayerData?> = MutableStateFlow(null)
    override val currentPlayerData = _currentPlayerData.asStateFlow()

    

}