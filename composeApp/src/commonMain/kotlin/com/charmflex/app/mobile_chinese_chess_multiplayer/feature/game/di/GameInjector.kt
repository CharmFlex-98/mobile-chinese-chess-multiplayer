package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.GameRoomViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.ui.battlelobby.BattleLobbyViewModel

interface GameInjector {
    fun getBattleLobbyViewModel(): BattleLobbyViewModel
    fun getGameRoomViewModel(): GameRoomViewModel
}
