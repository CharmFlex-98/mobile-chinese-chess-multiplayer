package com.charmflex.app.mobile_chinese_chess_multiplayer.di
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.di.androidCoreModule
import org.koin.core.module.Module

actual fun platformModules(): List<Module> {
    return listOf(
        androidCoreModule()
    )
}