package com.charmflex.app.mobile_chinese_chess_multiplayer.di

import KoinInitializer
import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration


internal class AndroidKoinInitializer(
    private val appContext: Context,
) : KoinInitializer {
    override fun initialize(nativeDependencyProvider: NativeDependencyProvider?) {
        startKoin {
            internalInit()
        }
    }

    override fun initAsync(): KoinConfiguration {
        return koinConfiguration {
            internalInit()
        }
    }

    private fun KoinApplication.internalInit() {
        androidContext(appContext)
        // Android-specific
//        modules(createFirstModule, authModuleAndroid, androidBackupModules, androidMainModule)
        // Common
        modules(commonModules())
        modules(platformModules())
    }
}