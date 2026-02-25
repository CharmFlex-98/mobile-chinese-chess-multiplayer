package com.charmflex.app.mobile_chinese_chess_multiplayer.core.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ToastManager
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.utils.DeepLinkHandler

interface CoreInjector {
    fun provideRouteNavigator(): RouteNavigator
    fun provideToastManager(): ToastManager
    fun provideDeeplinkHandler(): DeepLinkHandler
}