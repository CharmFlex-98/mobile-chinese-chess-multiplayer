package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.GameRepository
import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.ai.AiEngine
import com.charmflex.xiangqi.engine.rules.GameRules
import com.charmflex.xiangqi.engine.rules.MoveGenerator
import com.charmflex.xiangqi.engine.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory


@Factory
class GameRoomViewModel(
    private val gameRepository: GameRepository,
    private val routeNavigator: RouteNavigator
) : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var aiEngine: AiEngine? = null
    private var aiColor: PieceColor = PieceColor.BLACK
    private var onlineEventsJob: Job? = null
    private var reconnectJob: Job? = null

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

    fun quitConfirmation(show: Boolean) {
        _state.update {
            it.copy(
                quitConfirmation = show
            )
        }
    }

    fun abandonGame() {
        viewModelScope.launch {
            if (_state.value.gameMode == GameMode.ONLINE) {
                _state.value.onlineInfo?.roomId?.let {
                    gameRepository.abandonGame(it)
                    routeNavigator.pop()
                }
            }
        }

    }

    fun startSpectating(roomId: String, redPlayerName: String, blackPlayerName: String) {
        println("[GAME] startSpectating room=$roomId red=$redPlayerName black=$blackPlayerName")
        aiEngine = null
        _state.value = GameState(
            gameMode = GameMode.ONLINE,
            isSpectator = true,
            spectatorRedPlayerName = redPlayerName,
            spectatorBlackPlayerName = blackPlayerName,
            onlineInfo = OnlineGameInfo(
                roomId = roomId,
                opponentName = redPlayerName,
                playerColor = PieceColor.RED,
                connectionState = ConnectionState.CONNECTED
            )
        )
        viewModelScope.launch {
            gameRepository.watchRoom(roomId)
        }
        observeOnlineEvents(roomId)
        observeConnectionForReconnect(roomId)
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
            waitingForOpponent = isCreator
        )
        viewModelScope.launch {
            println("[GAME] Sending room_join WS for room=$roomId")
            gameRepository.joinRoomWs(roomId)
        }
        observeOnlineEvents(roomId)
        observeConnectionForReconnect(roomId)
    }

    private fun observeConnectionForReconnect(roomId: String) {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            var wasConnected = true
            gameRepository.isConnected().collect { connected ->
                if (!connected) {
                    wasConnected = false
                } else if (!wasConnected) {
                    // WS just came back — re-send room_join to get GAME_STATE from server
                    println("[GAME] WS reconnected, rejoining room=$roomId")
                    wasConnected = true
                    gameRepository.joinRoomWs(roomId)
                }
            }
        }
    }

    private fun observeOnlineEvents(roomId: String) {
        onlineEventsJob?.cancel()
        println("[GAME] Observing online events for room=$roomId")
        onlineEventsJob = viewModelScope.launch {
            gameRepository.subscribeMatchRoomEvents(roomId).collect { msg ->
                println("[GAME] Event received: ${msg::class.simpleName}")
                when (msg) {
                    is MoveMade -> {
                        println("[GAME] Opponent move: (${msg.move.fromRow},${msg.move.fromCol})->(${msg.move.toRow},${msg.move.toCol})")
                        handleOpponentMove(msg)
                    }
                    is GameOver -> {
                        println("[GAME] Game over: ${msg.result} reason=${msg.reason}")
                        handleGameOver(msg)
                    }
                    is TimerUpdate -> handleTimerUpdate(msg)
                    is ChatReceive -> {
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
                    is OpponentJoined -> {
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
                    is GameStarted -> {
                        println("[GAME] Game started!")
                        _state.update {
                            it.copy(waitingForOpponent = false)
                        }
                    }
                    is RoomSnapshot -> {
                        println("[GAME] Room snapshot: ${msg.moves.size} moves, red=${msg.redPlayer.name} black=${msg.blackPlayer.name}")
                        var board = Board.initial()
                        var currentTurn = PieceColor.RED
                        val moveHistory = mutableListOf<Move>()
                        for (moveDto in msg.moves) {
                            val from = Position(moveDto.fromRow, moveDto.fromCol)
                            val to = Position(moveDto.toRow, moveDto.toCol)
                            val piece = board[from] ?: continue
                            val captured = board[to]
                            val move = Move(from = from, to = to, piece = piece, captured = captured)
                            board = board.applyMove(move)
                            moveHistory.add(move)
                            currentTurn = currentTurn.opponent
                        }
                        val status = GameRules.getGameStatus(board, currentTurn)
                        _state.update {
                            it.copy(
                                board = board,
                                currentTurn = currentTurn,
                                moveHistory = moveHistory.toList(),
                                status = status,
                                spectatorRedPlayerName = msg.redPlayer.name,
                                spectatorBlackPlayerName = msg.blackPlayer.name,
                                onlineInfo = it.onlineInfo?.copy(
                                    redTimeMillis = msg.redTimeMillis,
                                    blackTimeMillis = msg.blackTimeMillis
                                )
                            )
                        }
                    }
                    is DrawOffered -> {
                        println("[GAME] Draw offered by ${msg.offeredBy}")
                        _state.update { it.copy(drawOffered = true) }
                    }
                    is OpponentDisconnected -> {
                        println("[GAME] Opponent disconnected!")
                        _state.update {
                            it.copy(
                                onlineInfo = it.onlineInfo?.copy(
                                    connectionState = ConnectionState.RECONNECTING
                                )
                            )
                        }
                    }
                    is OpponentReconnected -> {
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

    private fun handleOpponentMove(msg: MoveMade) {
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

    private fun handleGameOver(msg: GameOver) {
        val status = when (msg.result) {
            "red_wins" -> GameStatus.RED_WINS
            "black_wins" -> GameStatus.BLACK_WINS
            "draw" -> GameStatus.DRAW
            else -> return
        }
        _state.update { it.copy(status = status) }
    }

    private fun handleTimerUpdate(msg: TimerUpdate) {
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
        if (currentState.isSpectator) return
        if (currentState.status != GameStatus.PLAYING) return
        if (currentState.aiThinking) return
        if (currentState.waitingForOpponent) return

        if (currentState.gameMode == GameMode.VS_AI && currentState.currentTurn == aiColor) return

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

        if (currentState.gameMode == GameMode.ONLINE) {
            val roomId = currentState.onlineInfo?.roomId ?: return
            viewModelScope.launch {
                println("[GAME] Sending move to server: room=$roomId")
                gameRepository.sendMove(roomId, move)
                // Server detects checkmate/stalemate/timeout server-side and broadcasts GAME_OVER.
                // The client must not report game-over; it waits for the authoritative server signal.
            }
        }

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
            gameRepository.resign(roomId)
        }
    }

    fun offerDraw() {
        val roomId = _state.value.onlineInfo?.roomId ?: return
        viewModelScope.launch {
            gameRepository.offerDraw(roomId)
        }
    }

    fun respondToDraw(accepted: Boolean) {
        val roomId = _state.value.onlineInfo?.roomId ?: return
        _state.update { it.copy(drawOffered = false) }
        viewModelScope.launch {
            gameRepository.respondToDraw(roomId, accepted)
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
            gameRepository.sendChat(roomId, message)
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
        reconnectJob?.cancel()
    }
}
