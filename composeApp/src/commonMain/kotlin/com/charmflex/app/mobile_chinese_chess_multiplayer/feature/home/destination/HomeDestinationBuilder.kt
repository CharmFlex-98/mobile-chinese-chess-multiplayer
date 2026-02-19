package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.destination

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.DestinationBuilder
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.BackgroundDeepDark
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.BackgroundDark
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.BottomNavigationBar
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.getViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependencies
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependenciesProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.route.AuthRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.route.GameRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.mainmenu.MainMenuScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.route.HomeRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.settings.SettingsScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.social.SocialScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.ui.battlelobby.BattleLobbyScreen
import com.charmflex.xiangqi.engine.model.PieceColor

class HomeDestinationBuilder : DestinationBuilder {
    private val appDependencies: AppDependencies by lazy {
        AppDependenciesProvider.instance.provideAppDependencies()
    }

    override fun NavGraphBuilder.buildGraph() {
        composable<HomeRoute.ROOT> {
            val mainMenuViewModel = getViewModel { appDependencies.getMainMenuViewModel() }
            val battleLobbyViewModel = getViewModel { appDependencies.getBattleLobbyViewModel() }
            val gameRoomViewModel = remember { appDependencies.getGameRoomViewModel() }
            val routeNavigator = remember { appDependencies.provideRouteNavigator() }
            val sessionManager = remember { appDependencies.sessionManager() }
            val authService = remember { appDependencies.provideAuthService() }

            var currentTab by remember { mutableStateOf("home") }

            Scaffold(
                containerColor = if (currentTab == "social") BackgroundDeepDark else BackgroundDark,
                bottomBar = {
                    BottomNavigationBar(
                        currentRoute = currentTab,
                        onNavigate = { currentTab = it }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (currentTab) {
                        "home" -> MainMenuScreen(
                            viewModel = mainMenuViewModel,
                            onNavigateToSocial = { currentTab = "social" },
                            onNavigateToBattle = { currentTab = "battle" }
                        )

                        "battle" -> BattleLobbyScreen(
                            viewModel = battleLobbyViewModel,
                            onNavigateToGame = { roomId, opponentName, playerColor, isCreator ->
                                val color =
                                    if (playerColor == "RED") PieceColor.RED else PieceColor.BLACK
                                routeNavigator.navigateTo(
                                    GameRoute.Match.Online(
                                        roomId, opponentName, color, isCreator
                                    )
                                )
                            }
                        )

                        "social" -> SocialScreen()
                        "settings" -> SettingsScreen(
                            isGuest = sessionManager.currentUserSession.value?.isGuest == true,
                            onSignOut = {
                                authService.logout()
                                routeNavigator.navigateAndPopUpTo(AuthRoute.Login, HomeRoute.ROOT)
                            }
                        )
                    }
                }
            }
        }
    }
}
