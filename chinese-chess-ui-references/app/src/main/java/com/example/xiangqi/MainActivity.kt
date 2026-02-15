package com.example.xiangqi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.xiangqi.ui.BottomNavigationBar
import com.example.xiangqi.ui.screens.BattleLobbyScreen
import com.example.xiangqi.ui.screens.GameRoomScreen
import com.example.xiangqi.ui.screens.MainMenuScreen
import com.example.xiangqi.ui.screens.SocialScreen
import com.example.xiangqi.ui.theme.XiangqiMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XiangqiMasterTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route != currentRoute) {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") { MainMenuScreen() }
                            composable("battle") { BattleLobbyScreen() }
                            composable("social") { SocialScreen() }
                            composable("settings") { GameRoomScreen() }
                        }
                    }
                }
            }
        }
    }
}
