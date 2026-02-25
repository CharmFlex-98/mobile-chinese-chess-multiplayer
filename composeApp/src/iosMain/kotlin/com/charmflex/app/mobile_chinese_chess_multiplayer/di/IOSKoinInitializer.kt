package com.charmflex.app.mobile_chinese_chess_multiplayer.di

import KoinInitializer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration

class IOSKoinInitializer : KoinInitializer {
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
        modules(commonModules())
        modules(platformModules())
    }
}