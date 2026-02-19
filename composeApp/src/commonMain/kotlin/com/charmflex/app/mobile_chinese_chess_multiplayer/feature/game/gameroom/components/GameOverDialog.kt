package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.xiangqi.engine.model.GameStatus

@Composable
fun GameOverDialog(
    status: GameStatus,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = Color.White,
        title = {
            Text(
                text = when (status) {
                    GameStatus.RED_WINS -> "Red Wins!"
                    GameStatus.BLACK_WINS -> "Black Wins!"
                    GameStatus.DRAW -> "Draw!"
                    else -> ""
                },
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = GoldPrimary
            )
        },
        text = {
            Text(
                text = when (status) {
                    GameStatus.RED_WINS -> "Red has achieved checkmate."
                    GameStatus.BLACK_WINS -> "Black has achieved checkmate."
                    GameStatus.DRAW -> "The game ended in a draw."
                    else -> ""
                },
                color = TextGray
            )
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("New Game", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextGray)
            }
        }
    )
}
