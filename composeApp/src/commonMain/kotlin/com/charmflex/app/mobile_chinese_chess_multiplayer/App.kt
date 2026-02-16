package com.charmflex.app.mobile_chinese_chess_multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.di.AppModule
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.Route
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.model.PieceColor
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.battlelobby.BattleLobbyScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.battlelobby.BattleLobbyViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.common.BottomNavigationBar
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.gameroom.GameRoomScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.gameroom.GameRoomViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.gameroom.components.AiDifficultySelector
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.login.LoginScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.login.LoginViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.mainmenu.MainMenuScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.mainmenu.MainMenuViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.settings.SettingsScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.social.SocialScreen

@Composable
fun App() {
    XiangqiMasterTheme {
        var currentRoute by remember { mutableStateOf(Route.Login.route) }
        var isRestoringSession by remember { mutableStateOf(true) }

        val deps = remember { AppModule.instance }

        // Try to restore session on launch
        LaunchedEffect(Unit) {
            val restored = deps.userRepository.restoreSession()
            if (restored) {
                currentRoute = Route.Home.route
            }
            isRestoringSession = false
        }

        // Show loading while restoring session
        if (isRestoringSession) {
            Box(
                modifier = Modifier.fillMaxSize().background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
            return@XiangqiMasterTheme
        }

        // Login screen
        if (currentRoute == Route.Login.route) {
            val loginViewModel: LoginViewModel = viewModel {
                LoginViewModel(userRepository = deps.userRepository)
            }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { currentRoute = Route.Home.route }
            )
            return@XiangqiMasterTheme
        }

        val gameViewModel: GameRoomViewModel = viewModel {
            GameRoomViewModel(gameRepository = deps.gameRepository)
        }
        val battleLobbyViewModel: BattleLobbyViewModel = viewModel {
            BattleLobbyViewModel(
                gameRepository = deps.gameRepository,
                userRepository = deps.userRepository
            )
        }
        val mainMenuViewModel: MainMenuViewModel = viewModel {
            MainMenuViewModel(userRepository = deps.userRepository)
        }

        // Full-screen routes (no bottom nav)
        when (currentRoute) {
            Route.GameRoom.route -> {
                GameRoomScreen(
                    viewModel = gameViewModel,
                    onBack = { currentRoute = Route.Home.route }
                )
                return@XiangqiMasterTheme
            }
            Route.AiSelect.route -> {
                AiSelectionScreen(
                    onSelect = { difficulty ->
                        gameViewModel.startAiGame(difficulty)
                        currentRoute = Route.GameRoom.route
                    },
                    onBack = { currentRoute = Route.Home.route }
                )
                return@XiangqiMasterTheme
            }
        }

        // Tab routes with bottom nav
        Scaffold(
            containerColor = when (currentRoute) {
                Route.Social.route -> BackgroundDeepDark
                else -> BackgroundDark
            },
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { currentRoute = it }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentRoute) {
                    Route.Home.route -> MainMenuScreen(
                        viewModel = mainMenuViewModel,
                        onNavigateToGame = {
                            gameViewModel.startLocalGame()
                            currentRoute = Route.GameRoom.route
                        },
                        onNavigateToAi = { currentRoute = Route.AiSelect.route },
                        onNavigateToSocial = { currentRoute = Route.Social.route },
                        onNavigateToBattle = { currentRoute = Route.Battle.route }
                    )
                    Route.Battle.route -> BattleLobbyScreen(
                        viewModel = battleLobbyViewModel,
                        onNavigateToGame = { roomId, opponentName, playerColor, isCreator ->
                            val color = if (playerColor == "RED") PieceColor.RED else PieceColor.BLACK
                            gameViewModel.startOnlineGame(roomId, opponentName, color, isCreator)
                            currentRoute = Route.GameRoom.route
                        }
                    )
                    Route.Social.route -> SocialScreen()
                    Route.Settings.route -> SettingsScreen(
                        isGuest = deps.userRepository.isGuest,
                        onSignOut = {
                            deps.userRepository.logout()
                            currentRoute = Route.Login.route
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSelectionScreen(
    onSelect: (AiDifficulty) -> Unit,
    onBack: () -> Unit
) {
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
                TextButton(onClick = onBack) {
                    Text("< Back", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text("AI Practice", style = AppTypography.titleLarge, color = Color.White)
            }
            AiDifficultySelector(onSelect = onSelect)
        }
    }
}
