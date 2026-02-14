package com.example.xiangqi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xiangqi.ui.theme.*

@Composable
fun ChinesePatternBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val dotRadius = 1.dp.toPx()
                val spacing = 24.dp.toPx()
                val color = GoldPrimary.copy(alpha = 0.05f)

                var x = 2.dp.toPx()
                while (x < size.width) {
                    var y = 2.dp.toPx()
                    while (y < size.height) {
                        drawCircle(
                            color = color,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }
    ) {
        content()
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = BackgroundDeepDark.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        val items = listOf(
            NavigationItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavigationItem("battle", "Battle", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
            NavigationItem("social", "Social", Icons.Filled.Group, Icons.Outlined.Group),
            NavigationItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )

        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label,
                        tint = if (selected) GoldPrimary else TextGray
                    )
                },
                label = {
                    Text(
                        text = item.label.uppercase(),
                        style = Typography.labelSmall,
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

data class NavigationItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

@Composable
fun XiangqiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = GoldPrimary,
    contentColor: Color = Color.Black,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = Typography.labelLarge
            )
        }
    }
}
