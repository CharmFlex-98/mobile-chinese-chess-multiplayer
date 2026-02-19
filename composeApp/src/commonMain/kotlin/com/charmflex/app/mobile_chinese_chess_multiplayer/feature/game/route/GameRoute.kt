package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.route

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.NavigationRoute
import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.model.PieceColor
import kotlinx.serialization.Serializable

object GameRoute {
    @Serializable
    sealed interface Match : NavigationRoute {
        @Serializable
        data class Bot(
            val difficulty: AiDifficulty
        ) : Match

        @Serializable
        data class Online(
            val roomId: String,
            val opponentName: String,
            val playerColor: PieceColor,
            val isCreator: Boolean
        ) : Match
    }
    @Serializable
    object AiSelect : NavigationRoute
}
