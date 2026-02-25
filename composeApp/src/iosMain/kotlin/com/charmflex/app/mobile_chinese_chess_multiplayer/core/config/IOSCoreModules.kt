package com.charmflex.app.mobile_chinese_chess_multiplayer.core.config
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.IOSSharedPreferencesFactory
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.SharedPrefsFactory
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun iOSCoreModule(): Module {
    return module {
        singleOf(::IOSAppConfigProvider) { bind<AppConfigProvider>() }
        singleOf(::IOSSharedPreferencesFactory) { bind<SharedPrefsFactory>() }
    }
}