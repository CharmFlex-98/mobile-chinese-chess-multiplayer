package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.gameroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.MoveDto
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.WsServerMessage
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.GameRepository
import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.ai.AiEngine
import com.charmflex.xiangqi.engine.rules.GameRules
import com.charmflex.xiangqi.engine.rules.MoveGenerator
import com.charmflex.xiangqi.engine.rules.MoveValidator
import com.charmflex.xiangqi.engine.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameRoomViewModel(
    private val gameRepository: GameRepository? = null
) : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var aiEngine: AiEngine? = null
    private var aiColor: PieceColor = PieceColor.BLACK
    private var onlineEventsJob: Job? = null

    fun startLocalGame() {
        _state.value = GameState(gameMode = GameMode.LOCAL_2P)
        aiEngine = null
        onlineEventsJob?.cancel()
    }

    fun startAiGame(difficulty: AiDifficulty, playerColor: PieceColor = PieceColor.RED) {
        onlineEventsJob?.cancel()
        aiColor = playerColor.opponent
        aiEngine = AiEngine(difficulty)
        _state.value = GameState(gameMode = GameMode.VS_AI)

        if (aiColor == PieceColor.RED) {
            triggerAiMove()
        }
    }

    fun startOnlineGame(roomId: String, opponentName: String, playerColor: PieceColor, isCreator: Boolean = false) {
        println("[GAME] startOnlineGame room=$roomId opponent=$opponentName color=$playerColor isCreator=$isCreator")
        aiEngine = null
        _state.value = GameState(
            gameMode = GameMode.ONLINE,
            onlineInfo = OnlineGameInfo(
                roomId = roomId,
                opponentName = opponentName,
                playerColor = playerColor,
                connectionState = ConnectionState.CONNECTED
            ),
            // Only room creator waits for opponent to join
            waitingForOpponent = isCreator
        )
        // Register WS session for this room so broadcasts work
        viewModelScope.launch {
            println("[GAME] Sending room_join WS for room=$roomId")
            gameRepository?.joinRoomWs(roomId)
        }
        observeOnlineEvents(roomId)
    }

    private fun observeOnlineEvents(roomId: String) {
        val repo = gameRepository ?: return
        onlineEventsJob?.cancel()
        println("[GAME] Observing online events for room=$roomId")
        onlineEventsJob = viewModelScope.launch {
            repo.observeGameEvents(roomId).collect { msg ->
                println("[GAME] Event received: ${msg::class.simpleName}")
                when (msg) {
                    is WsServerMessage.MoveMade -> {
                        println("[GAME] Opponent move: (${msg.move.fromRow},${msg.move.fromCol})->(${msg.move.toRow},${msg.move.toCol})")
                        handleOpponentMove(msg)
                    }
                    is WsServerMessage.GameOver -> {
                        println("[GAME] Game over: ${msg.result} reason=${msg.reason}")
                        handleGameOver(msg)
                    }
                    is WsServerMessage.TimerUpdate -> handleTimerUpdate(msg)
                    is WsServerMessage.ChatReceive -> {
                        println("[GAME] Chat from ${msg.senderName}: ${msg.message}")
                        _state.update {
                            it.copy(
                                chatMessages = it.chatMessages + ChatMessage(
                                    senderId = msg.senderId,
                                    senderName = msg.senderName,
                                    message = msg.message,
                                    timestamp = msg.timestamp,
                                    isFromMe = false
                                )
                            )
                        }
                    }
                    is WsServerMessage.OpponentJoined -> {
                        println("[GAME] Opponent joined: ${msg.opponent.name}")
                        _state.update {
                            it.copy(
                                onlineInfo = it.onlineInfo?.copy(
                                    opponentName = msg.opponent.name,
                                    connectionState = ConnectionState.CONNECTED
                                ),
                                waitingForOpponent = false
                            )
                        }
                    }
                    is WsServerMessage.GameStarted -> {
                        println("[GAME] Game started!")
                        _state.update {
                            it.copy(waitingForOpponent = false)
                        }
                    }
                    is WsServerMessage.DrawOffered -> {
                        println("[GAME] Draw offered by ${msg.offeredBy}")
                        _state.update { it.copy(drawOffered = true) }
                    }
                    is WsServerMessage.OpponentDisconnected -> {
                        println("[GAME] Opponent disconnected!")
                        _state.update {
                            it.copy(
                                onlineInfo = it.onlineInfo?.copy(
                                    connectionState = ConnectionState.RECONNECTING
                                )
                            )
                        }
                    }
                    is WsServerMessage.OpponentReconnected -> {
                        println("[GAME] Opponent reconnected!")
                        _state.update {
                            it.copy(
                                onlineInfo = it.onlineInfo?.copy(
                                    connectionState = ConnectionState.CONNECTED
                                )
                            )
                        }
                    }
                    else -> {
                        println("[GAME] Unhandled event: ${msg::class.simpleName}")
                    }
                }
            }
        }
    }

    private fun handleOpponentMove(msg: WsServerMessage.MoveMade) {
        val from = Position(msg.move.fromRow, msg.move.fromCol)
        val to = Position(msg.move.toRow, msg.move.toCol)
        val board = _state.value.board
        val piece = board[from] ?: run {
            println("[GAME] WARN: No piece at $from for opponent move!")
            return
        }
        val captured = board[to]

        val move = Move(from = from, to = to, piece = piece, captured = captured)
        val newBoard = board.applyMove(move)
        val nextTurn = _state.value.currentTurn.opponent
        val status = GameRules.getGameStatus(newBoard, nextTurn)
        println("[GAME] Applied opponent move: ${piece.type} $from->$to, nextTurn=$nextTurn status=$status")

        _state.update {
            it.copy(
                board = newBoard,
                currentTurn = nextTurn,
                moveHistory = it.moveHistory + move,
                status = status,
                selectedPosition = null,
                validMoves = emptyList()
            )
        }
    }

    private fun handleGameOver(msg: WsServerMessage.GameOver) {
        val status = when (msg.result) {
            "red_wins" -> GameStatus.RED_WINS
            "black_wins" -> GameStatus.BLACK_WINS
            "draw" -> GameStatus.DRAW
            else -> return
        }
        _state.update { it.copy(status = status) }
    }

    private fun handleTimerUpdate(msg: WsServerMessage.TimerUpdate) {
        _state.update {
            it.copy(
                onlineInfo = it.onlineInfo?.copy(
                    redTimeMillis = msg.redTimeMillis,
                    blackTimeMillis = msg.blackTimeMillis
                )
            )
        }
    }

    fun onBoardTap(pos: Position) {
        val currentState = _state.value
        if (currentState.status != GameStatus.PLAYING) return
        if (currentState.aiThinking) return
        if (currentState.waitingForOpponent) return

        // In VS_AI mode, only allow taps during human's turn
        if (currentState.gameMode == GameMode.VS_AI && currentState.currentTurn == aiColor) return

        // In ONLINE mode, only allow taps during player's turn
        if (currentState.gameMode == GameMode.ONLINE) {
            val playerColor = currentState.onlineInfo?.playerColor ?: return
            if (currentState.currentTurn != playerColor) return
        }

        val board = currentState.board
        val tappedPiece = board[pos]

        val selectedPos = currentState.selectedPosition
        if (selectedPos != null) {
            if (pos in currentState.validMoves) {
                makeMove(selectedPos, pos)
                return
            }
            if (tappedPiece != null && tappedPiece.color == currentState.currentTurn) {
                selectPiece(pos)
                return
            }
            _state.update { it.copy(selectedPosition = null, validMoves = emptyList()) }
            return
        }

        if (tappedPiece != null && tappedPiece.color == currentState.currentTurn) {
            selectPiece(pos)
        }
    }

    private fun selectPiece(pos: Position) {
        val board = _state.value.board
        val legalMoves = MoveGenerator.generateLegalMovesFrom(board, pos)
        _state.update {
            it.copy(
                selectedPosition = pos,
                validMoves = legalMoves.map { move -> move.to }
            )
        }
    }

    private fun makeMove(from: Position, to: Position) {
        val currentState = _state.value
        val board = currentState.board
        val piece = board[from] ?: return
        val captured = board[to]

        val move = Move(from = from, to = to, piece = piece, captured = captured)
        val newBoard = board.applyMove(move)
        val nextTurn = currentState.currentTurn.opponent
        val status = GameRules.getGameStatus(newBoard, nextTurn)

        if (currentState.gameMode == GameMode.ONLINE) {
            println("[GAME] Making online move: ${piece.type} $from->$to")
        }

        _state.update {
            it.copy(
                board = newBoard,
                currentTurn = nextTurn,
                moveHistory = it.moveHistory + move,
                status = status,
                selectedPosition = null,
                validMoves = emptyList()
            )
        }

        // Send move to server in online mode
        if (currentState.gameMode == GameMode.ONLINE) {
            val roomId = currentState.onlineInfo?.roomId ?: return
            viewModelScope.launch {
                println("[GAME] Sending move to server: room=$roomId")
                gameRepository?.sendMove(roomId, move)
                // Report game-over to server if checkmate/stalemate
                if (status != GameStatus.PLAYING) {
                    val result = when (status) {
                        GameStatus.RED_WINS -> "red_wins"
                        GameStatus.BLACK_WINS -> "black_wins"
                        GameStatus.DRAW -> "draw"
                        else -> return@launch
                    }
                    println("[GAME] Reporting game over to server: $result")
                    gameRepository?.reportGameOver(roomId, result, "checkmate")
                }
            }
        }

        // Trigger AI move if it's AI's turn and game is still playing
        if (status == GameStatus.PLAYING && _state.value.gameMode == GameMode.VS_AI && nextTurn == aiColor) {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        val engine = aiEngine ?: return
        _state.update { it.copy(aiThinking = true) }

        viewModelScope.launch {
            val currentState = _state.value
            val bestMove = withContext(Dispatchers.Default) {
                engine.findBestMove(currentState.board, aiColor)
            }

            if (bestMove != null) {
                val board = _state.value.board
                val newBoard = board.applyMove(bestMove)
                val nextTurn = aiColor.opponent
                val status = GameRules.getGameStatus(newBoard, nextTurn)

                _state.update {
                    it.copy(
                        board = newBoard,
                        currentTurn = nextTurn,
                        moveHistory = it.moveHistory + bestMove,
                        status = status,
                        selectedPosition = null,
                        validMoves = emptyList(),
                        aiThinking = false
                    )
                }
            } else {
                _state.update { it.copy(aiThinking = false) }
            }
        }
    }

    fun resignOnline() {
        val roomId = _state.value.onlineInfo?.roomId ?: return
        println("[GAME] Resigning in room $roomId")
        viewModelScope.launch {
            gameRepository?.resign(roomId)
        }
    }

    fun offerDraw() {
        val roomId = _state.value.onlineInfo?.roomId ?: return
        viewModelScope.launch {
            gameRepository?.offerDraw(roomId)
        }
    }

    fun respondToDraw(accepted: Boolean) {
        val roomId = _state.value.onlineInfo?.roomId ?: return
        _state.update { it.copy(drawOffered = false) }
        viewModelScope.launch {
            gameRepository?.respondToDraw(roomId, accepted)
        }
    }

    fun sendChatMessage(message: String) {
        val roomId = _state.value.onlineInfo?.roomId ?: return
        _state.update {
            it.copy(
                chatMessages = it.chatMessages + ChatMessage(
                    senderId = "me",
                    senderName = "You",
                    message = message,
                    timestamp = 0L,
                    isFromMe = true
                )
            )
        }
        viewModelScope.launch {
            gameRepository?.sendChat(roomId, message)
        }
    }

    fun resetGame() {
        onlineEventsJob?.cancel()
        if (_state.value.gameMode == GameMode.VS_AI) {
            val engine = aiEngine
            if (engine != null) {
                _state.value = GameState(gameMode = GameMode.VS_AI)
                if (aiColor == PieceColor.RED) {
                    triggerAiMove()
                }
            } else {
                _state.value = GameState()
            }
        } else {
            _state.value = GameState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        onlineEventsJob?.cancel()
    }
}
