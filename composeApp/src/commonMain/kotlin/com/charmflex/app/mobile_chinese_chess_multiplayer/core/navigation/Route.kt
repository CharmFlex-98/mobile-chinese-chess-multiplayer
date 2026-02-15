package com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Battle : Route("battle")
    data object Social : Route("social")
    data object Settings : Route("settings")
    data object GameRoom : Route("game_room")
    data object AiSelect : Route("ai_select")
}
