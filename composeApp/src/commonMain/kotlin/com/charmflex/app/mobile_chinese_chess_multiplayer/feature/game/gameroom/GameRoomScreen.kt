package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom

import androidx.compose.foundation.BorderStroke
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
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ChatPanel
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ConnectionBanner
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.PlayerHeader
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.formatTime
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components.GameOverDialog
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components.MoveLog
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components.XiangqiBoard
import com.charmflex.xiangqi.engine.rules.MoveValidator
import com.charmflex.xiangqi.engine.model.*

@Composable
fun GameRoomScreen(
    viewModel: GameRoomViewModel,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    var showGameOverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.status) {
        if (state.status != GameStatus.PLAYING) {
            showGameOverDialog = true
        }
    }

    val isOnline = state.gameMode == GameMode.ONLINE
    val onlineInfo = state.onlineInfo
    // Flip the board when the local player is BLACK in online mode
    val isFlipped = isOnline && onlineInfo?.playerColor == PieceColor.BLACK

    Scaffold(
        containerColor = BackgroundDarkAlt
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Connection banner for online games
            if (isOnline && onlineInfo != null) {
                ConnectionBanner(onlineInfo.connectionState)
            }

            // Top player header: opponent's side (normally BLACK, RED when flipped)
            val topColor = if (isFlipped) PieceColor.RED else PieceColor.BLACK
            val bottomColor = if (isFlipped) PieceColor.BLACK else PieceColor.RED

            PlayerHeader(
                name = when {
                    isOnline && onlineInfo != null -> {
                        if (onlineInfo.playerColor == topColor) "You" else onlineInfo.opponentName
                    }
                    state.gameMode == GameMode.VS_AI -> if (topColor == PieceColor.BLACK) "AI" else "You"
                    else -> if (topColor == PieceColor.BLACK) "Black" else "Red"
                },
                pieceChar = if (topColor == PieceColor.BLACK) "將" else "帥",
                color = topColor,
                isCurrentTurn = state.currentTurn == topColor,
                isInCheck = state.status == GameStatus.PLAYING &&
                        state.currentTurn == topColor &&
                        MoveValidator.isInCheck(state.board, topColor),
                isThinking = state.aiThinking && state.currentTurn == topColor,
                timerText = if (isOnline) formatTime(
                    if (topColor == PieceColor.RED) onlineInfo?.redTimeMillis ?: 0
                    else onlineInfo?.blackTimeMillis ?: 0
                ) else null
            )

            // Game Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                XiangqiBoard(
                    board = state.board,
                    selectedPosition = state.selectedPosition,
                    validMoves = state.validMoves,
                    lastMove = state.moveHistory.lastOrNull(),
                    onTap = viewModel::onBoardTap,
                    isFlipped = isFlipped
                )

                MoveLog(
                    moves = state.moveHistory,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 4.dp)
                )

                // Waiting for opponent overlay (room creator waiting)
                if (isOnline && state.waitingForOpponent && state.moveHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GoldPrimary)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Waiting for opponent to join...",
                                style = AppTypography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom player header: local player's side (normally RED, BLACK when flipped)
            PlayerHeader(
                name = when {
                    isOnline && onlineInfo != null -> {
                        if (onlineInfo.playerColor == bottomColor) "You" else onlineInfo.opponentName
                    }
                    state.gameMode == GameMode.VS_AI -> if (bottomColor == PieceColor.RED) "You" else "AI"
                    else -> if (bottomColor == PieceColor.RED) "Red" else "Black"
                },
                pieceChar = if (bottomColor == PieceColor.RED) "帥" else "將",
                color = bottomColor,
                isCurrentTurn = state.currentTurn == bottomColor,
                isInCheck = state.status == GameStatus.PLAYING &&
                        state.currentTurn == bottomColor &&
                        MoveValidator.isInCheck(state.board, bottomColor),
                isThinking = state.aiThinking && state.currentTurn == bottomColor,
                timerText = if (isOnline) formatTime(
                    if (bottomColor == PieceColor.RED) onlineInfo?.redTimeMillis ?: 0
                    else onlineInfo?.blackTimeMillis ?: 0
                ) else null
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBack != null) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Menu", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                if (isOnline) {
                    Button(
                        onClick = { viewModel.offerDraw() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Draw", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resignOnline() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Text("Resign", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.resetGame() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("New Game", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetGame() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                    ) {
                        Text("Resign", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showGameOverDialog && state.status != GameStatus.PLAYING) {
        GameOverDialog(
            status = state.status,
            onNewGame = {
                showGameOverDialog = false
                if (!isOnline) viewModel.resetGame()
                else onBack?.invoke()
            },
            onDismiss = { showGameOverDialog = false }
        )
    }

    // Draw offer dialog
    if (state.drawOffered) {
        AlertDialog(
            onDismissRequest = { viewModel.respondToDraw(false) },
            title = { Text("Draw Offer", color = Color.White) },
            text = { Text("Your opponent offers a draw. Accept?", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.respondToDraw(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) { Text("Accept", color = Color.Black) }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.respondToDraw(false) }) {
                    Text("Decline", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Chat panel (bottom sheet style)
    if (isOnline) {
        ChatPanel(
            messages = state.chatMessages,
            onSend = { viewModel.sendChatMessage(it) }
        )
    }
}
