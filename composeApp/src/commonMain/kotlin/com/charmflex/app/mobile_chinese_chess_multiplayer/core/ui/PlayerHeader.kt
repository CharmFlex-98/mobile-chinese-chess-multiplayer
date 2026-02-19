package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.xiangqi.engine.model.PieceColor

@Composable
fun PlayerHeader(
    name: String,
    pieceChar: String,
    color: PieceColor,
    isCurrentTurn: Boolean,
    isInCheck: Boolean,
    isThinking: Boolean = false,
    timerText: String? = null,
    isWaiting: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentTurn) SurfaceDarkAlt.copy(alpha = 0.8f)
                else SurfaceDarkAlt.copy(alpha = 0.3f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (isCurrentTurn) GoldPrimary else GoldPrimary.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .background(
                        if (color == PieceColor.RED) PieceRedBackground.copy(alpha = 0.3f)
                        else PieceBlackBackground
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    pieceChar,
                    color = if (color == PieceColor.RED) GoldPrimary else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    name,
                    style = AppTypography.titleMedium,
                    color = Color.White
                )
                if (isInCheck) {
                    Text(
                        "CHECK!",
                        style = AppTypography.labelSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Timer display for online games
            timerText?.let {
                Surface(
                    color = if (isCurrentTurn) GoldPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = AppTypography.titleMedium,
                        color = if (isCurrentTurn) GoldPrimary else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isThinking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AccentDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = GoldPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "THINKING...",
                        style = AppTypography.labelLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isWaiting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AccentDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = GoldPrimary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "WAITING...",
                        style = AppTypography.labelLarge,
                        color = GoldPrimary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isCurrentTurn) {
                Surface(
                    color = GoldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        "YOUR TURN",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = AppTypography.labelLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
