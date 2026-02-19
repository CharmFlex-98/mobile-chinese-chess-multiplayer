package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.data.repository.GameRepositoryImpl
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.GameRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.GameRoomViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.network.GameChannel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.ui.battlelobby.BattleLobbyViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val gameModule = module {
}
