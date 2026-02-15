package com.charmflex.xiangqi.engine.model

enum class GameStatus {
    PLAYING,
    RED_WINS,
    BLACK_WINS,
    DRAW
}

enum class GameMode {
    LOCAL_2P,
    VS_AI,
    ONLINE
}

data class OnlineGameInfo(
    val roomId: String = "",
    val opponentName: String = "",
    val playerColor: PieceColor = PieceColor.RED,
    val redTimeMillis: Long = 600_000L,
    val blackTimeMillis: Long = 600_000L,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING
}

data class ChatMessage(
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isFromMe: Boolean = false
)

data class GameState(
    val board: Board = Board.initial(),
    val currentTurn: PieceColor = PieceColor.RED,
    val moveHistory: List<Move> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val gameMode: GameMode = GameMode.LOCAL_2P,
    val aiThinking: Boolean = false,
    val onlineInfo: OnlineGameInfo? = null,
    val waitingForOpponent: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val drawOffered: Boolean = false
)
