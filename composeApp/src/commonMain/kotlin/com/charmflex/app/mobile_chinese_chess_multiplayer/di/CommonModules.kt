package com.charmflex.app.mobile_chinese_chess_multiplayer.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.di.coreModule
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.di.authModule
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.di.gameModule
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.di.homeModule
import org.koin.core.module.Module
import org.koin.ksp.generated.module

fun commonModules(): List<Module> {
    return listOf(
        coreModule(),
        authModule,
        homeModule,
        gameModule,
        AppModule().module
    )
}

expect fun platformModules(): List<Module>
