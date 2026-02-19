package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.destination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.DestinationBuilder
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.customNavType
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.AppTypography
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.BackgroundDark
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.GoldPrimary
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.SurfaceDark
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependenciesProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.GameRoomScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components.AiDifficultySelector
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.route.GameRoute
import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.model.PieceColor
import kotlin.reflect.typeOf

class GameDestination : DestinationBuilder {
    private val appDependencies by lazy { AppDependenciesProvider.instance.provideAppDependencies() }

    override fun NavGraphBuilder.buildGraph() {
        aiMatch()
        onlineMatch()
        aiSelect()
    }

    private fun NavGraphBuilder.aiMatch() {
        composable<GameRoute.Match.Bot>(
            typeMap = mapOf(typeOf<AiDifficulty?>() to customNavType<AiDifficulty>())
        ) {
            val gameRoomViewModel = remember { appDependencies.getGameRoomViewModel() }
            val route = it.toRoute<GameRoute.Match.Bot>()
            LaunchedEffect(Unit) {
                gameRoomViewModel.startAiGame(
                    route.difficulty
                )
            }
            val routeNavigator = remember { appDependencies.provideRouteNavigator() }
            GameRoomScreen(
                viewModel = gameRoomViewModel,
                onBack = { routeNavigator.pop() }
            )
        }
    }

    private fun NavGraphBuilder.onlineMatch() {
        composable<GameRoute.Match.Online>(
            typeMap = mapOf(typeOf<PieceColor?>() to customNavType<PieceColor>())
        ) {
            val gameRoomViewModel = remember { appDependencies.getGameRoomViewModel() }
            val route = it.toRoute<GameRoute.Match.Online>()
            LaunchedEffect(Unit) {
                gameRoomViewModel.startOnlineGame(
                    route.roomId,
                    route.opponentName,
                    route.playerColor,
                    route.isCreator
                )
            }
            val routeNavigator = remember { appDependencies.provideRouteNavigator() }
            GameRoomScreen(
                viewModel = gameRoomViewModel,
                onBack = { routeNavigator.pop() }
            )
        }
    }

    private fun NavGraphBuilder.aiSelect() {
        composable<GameRoute.AiSelect> {
            val gameRoomViewModel = remember { appDependencies.getGameRoomViewModel() }
            val routeNavigator = remember { appDependencies.provideRouteNavigator() }
            Scaffold(containerColor = BackgroundDark) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { routeNavigator.pop() }) {
                            Text("< Back", color = GoldPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("AI Practice", style = AppTypography.titleLarge, color = Color.White)
                    }
                    AiDifficultySelector(
                        onSelect = { difficulty ->
                            routeNavigator.navigateTo(GameRoute.Match.Bot(difficulty))
                        }
                    )
                }
            }
        }
    }
}
