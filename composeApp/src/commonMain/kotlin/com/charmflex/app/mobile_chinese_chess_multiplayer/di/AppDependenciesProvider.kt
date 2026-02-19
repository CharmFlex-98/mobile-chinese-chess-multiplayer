package com.charmflex.app.mobile_chinese_chess_multiplayer.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.di.CoreInjector
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ToastManager
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.di.AuthInjector
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.AuthService
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.AuthRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.ui.login.LoginViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.di.GameInjector
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.GameRoomViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.ui.battlelobby.BattleLobbyViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.di.HomeInjector
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.mainmenu.MainMenuViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.di.SessionInjector
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Module
@ComponentScan("com.charmflex.app.mobile_chinese_chess_multiplayer")
class AppModule


interface AppDependenciesProvider {
    companion object {
        lateinit var instance: AppDependenciesProvider
    }

    fun provideAppDependencies(): AppDependencies
}

class AppDependencies : KoinComponent, CoreInjector, HomeInjector, AuthInjector, GameInjector, SessionInjector {
    override fun getLoginViewModel(): LoginViewModel = get()
    override fun getMainMenuViewModel(): MainMenuViewModel = get()
    override fun provideRouteNavigator(): RouteNavigator = get()
    override fun provideToastManager(): ToastManager = get()
    override fun provideAuthService(): AuthService = get()
    override fun getBattleLobbyViewModel(): BattleLobbyViewModel = get()
    override fun getGameRoomViewModel(): GameRoomViewModel = get()
    override fun sessionManager(): SessionManager = get()
}
