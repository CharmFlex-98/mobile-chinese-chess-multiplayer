package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.AppTypography
import com.charmflex.xiangqi.engine.model.ConnectionState

@Composable
fun ConnectionBanner(connectionState: ConnectionState) {
    if (connectionState == ConnectionState.CONNECTED) return

    val (text, bgColor) = when (connectionState) {
        ConnectionState.CONNECTING -> "Connecting..." to Color(0xFF2196F3)
        ConnectionState.RECONNECTING -> "Opponent disconnected. Waiting..." to Color(0xFFFF9800)
        ConnectionState.DISCONNECTED -> "Disconnected" to Color(0xFFFF5252)
        ConnectionState.CONNECTED -> "" to Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AppTypography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
