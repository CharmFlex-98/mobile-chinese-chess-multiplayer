package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MoveDto(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int
)