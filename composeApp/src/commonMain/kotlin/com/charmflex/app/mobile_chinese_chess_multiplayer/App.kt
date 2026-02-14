package com.charmflex.app.mobile_chinese_chess_multiplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.BottomNavigationBar
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.screens.BattleLobbyScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.screens.GameRoomScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.screens.MainMenuScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.screens.SocialScreen
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.theme.XiangqiMasterTheme

@Composable
fun App() {
    var currentRoute by remember { mutableStateOf("home") }

    XiangqiMasterTheme {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        currentRoute = route
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentRoute) {
                    "home" -> MainMenuScreen()
                    "battle" -> BattleLobbyScreen()
                    "social" -> SocialScreen()
                    "settings" -> GameRoomScreen()
                }
            }
        }
    }
}
