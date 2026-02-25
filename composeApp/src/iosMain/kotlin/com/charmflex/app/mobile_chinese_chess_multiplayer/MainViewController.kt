package com.charmflex.app.mobile_chinese_chess_multiplayer

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependencies
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependenciesProvider
import platform.posix.exit

fun MainViewController() = ComposeUIViewController {
    val deps = remember { appDependencies.provideAppDependencies() }

    App(
        routeNavigator = deps.provideRouteNavigator(),
        toastManager = deps.provideToastManager()
    ) {
        exit(0)
    }
}

private val appDependencies: AppDependenciesProvider by lazy {
    DependencyProvider().also { AppDependenciesProvider.instance = it }
}


private class DependencyProvider : AppDependenciesProvider {

    private var appComponent: AppDependencies = AppDependencies()

    override fun provideAppDependencies(): AppDependencies {
        return appComponent ?: throw RuntimeException("KoinComponent is not yet initialized!")
    }
}
