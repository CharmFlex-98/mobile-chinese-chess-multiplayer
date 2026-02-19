package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*

data class NavItem(
    val route: String,
    val label: String,
    val symbol: String
)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem("home", "Home", "⌂"),
        NavItem("battle", "Battle", "⚔"),
        NavItem("social", "Social", "♟"),
        NavItem("settings", "Settings", "⚙")
    )

    NavigationBar(
        containerColor = BackgroundDeepDark.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Text(
                        item.symbol,
                        fontSize = 20.sp,
                        color = if (selected) GoldPrimary else TextGray
                    )
                },
                label = {
                    Text(
                        text = item.label.uppercase(),
                        style = AppTypography.labelSmall,
                        color = if (selected) GoldPrimary else TextGray,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
