package com.charmflex.app.mobile_chinese_chess_multiplayer.core.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.config.AndroidAppConfigProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.config.AppConfigProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.AndroidSharedPreferencesFactory
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.SharedPrefs
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.SharedPrefsFactory
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun androidCoreModule(): Module {
    return module {
        singleOf(::AndroidAppConfigProvider) { bind<AppConfigProvider>() }
        singleOf(::AndroidSharedPreferencesFactory) { bind<SharedPrefsFactory>() }
    }
}