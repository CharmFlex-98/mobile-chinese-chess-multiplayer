package com.example.xiangqi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = AccentDark,
    onSurfaceVariant = TextGray
)

@Composable
fun XiangqiMasterTheme(
    darkTheme: Boolean = true, // Always dark as per design
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
