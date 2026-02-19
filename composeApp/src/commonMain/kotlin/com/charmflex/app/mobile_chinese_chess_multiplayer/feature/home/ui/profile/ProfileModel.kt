package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.profile

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val xp: Int = 0,
    val level: Int = 1
)
