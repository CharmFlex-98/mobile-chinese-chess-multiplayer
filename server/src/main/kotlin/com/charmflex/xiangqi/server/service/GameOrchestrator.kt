package com.charmflex.xiangqi.server.service

import com.charmflex.xiangqi.engine.model.GameStatus
import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.websocket.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class GameOrchestrator(
    private val gameService: GameService,
    private val sessionRegistry: SessionRegistry,
    private val persistenceService: PlayerPersistenceService
) {
    private val log = LoggerFactory.getLogger(GameOrchestrator::class.java)
    // sessionId -> playerId
    private val sessionPlayerMap = ConcurrentHashMap<String, Player>()

    @Autowired
    @Lazy
    private lateinit var botService: BotService

    // -------------------------------------------------------------------------
    // Registration when player connects
    // -------------------------------------------------------------------------
    fun registerPlayerSession(sessionId: String, player: Player) {
        sessionPlayerMap[sessionId] = player

        val activeRoom = gameService.findActiveRoomForPlayer(player.id)
        if (activeRoom != null) {
            val playerColor = if (activeRoom.redPlayer?.id == player.id) "RED" else "BLACK"
            val opponentName = if (playerColor == "RED") activeRoom.blackPlayer?.name ?: "Opponent"
                               else activeRoom.redPlayer?.name ?: "Opponent"
            log.info("[ORCH] Player {} reconnected with active room {}, sending rejoin_available", player.name, activeRoom.id)
            sessionRegistry.sendToSession(sessionId, WsMessageBuilder.buildGameMessage(
                WsType.REJOIN_AVAILABLE,
                RejoinAvailablePayload(
                    roomId = activeRoom.id,
                    opponentName = opponentName,
                    playerColor = playerColor,
                    redTimeMillis = activeRoom.redTimeMillis,
                    blackTimeMillis = activeRoom.blackTimeMillis
                )
            ))
        }
    }

    // -------------------------------------------------------------------------
    // Bot-facing entry point
    // -------------------------------------------------------------------------

    /**
     * Called by BotService to apply a bot move, broadcast results, handle game-over,
     * and return the move result so BotService can decide on bot-vs-bot continuation.
     * Returns null if the move was rejected.
     */
    fun applyBotMove(roomId: String, move: MoveDto, botPlayerId: String): MakeMoveResult? {
        val result = gameService.makeMove(roomId, move, botPlayerId)
        if (!result.success) {
            log.warn("[ORCH] applyBotMove REJECTED: room={} bot={}", roomId, botPlayerId)
            return null
        }

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.MOVE_MADE,
            MoveMadePayload(roomId = roomId, move = move, playerId = botPlayerId)
        ), excludeSessionId = botPlayerId)

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.TIMER_UPDATE,
            TimerUpdatePayload(roomId = roomId, redTimeMillis = result.redTimeMillis, blackTimeMillis = result.blackTimeMillis)
        ))

        val isGameOver = result.timedOut || result.gameStatus != GameStatus.PLAYING
        if (isGameOver) {
            val gameOverResult = when {
                result.timedOut -> if (result.redTimeMillis <= 0) "black_wins" else "red_wins"
                else -> when (result.gameStatus) {
                    GameStatus.RED_WINS -> "red_wins"
                    GameStatus.BLACK_WINS -> "black_wins"
                    GameStatus.DRAW -> "draw"
                    else -> return result
                }
            }
            val reason = if (result.timedOut) "timeout" else "checkmate"
            log.info("[ORCH] Bot game over in room {}: {} ({})", roomId, gameOverResult, reason)
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.GAME_OVER,
                GameOverPayload(roomId = roomId, result = gameOverResult, reason = reason)
            ))
            grantXpForGameOver(roomId, gameOverResult)
            finishRoom(roomId)
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Connection lifecycle
    // -------------------------------------------------------------------------

    fun onConnectionClosed(sessionId: String) {
        sessionPlayerMap.remove(sessionId)
        // Remove ongoing matchmaking
        gameService.removeMatchMakingQueue(sessionId)
        botService.onPlayerQueueLeave(sessionId)

        // Remove the session Id from each room that contains it
        val roomIds = gameService.getRoomIdsForSession(sessionId)
        roomIds.forEach { roomId ->
            gameService.removeSessionIdFromRoom(roomId, sessionId)
            log.info("[ORCH] Notifying room {} about disconnection of session {}", roomId, sessionId)
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.OPPONENT_DISCONNECTED,
                OpponentDisconnectedPayload(roomId = roomId)
            ), excludeSessionId = sessionId)
            if (gameService.roomHasBot(roomId)) {
                botService.pauseBotGame(roomId)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Message handlers
    // -------------------------------------------------------------------------

    fun handleMessage(sessionId: String, scene: String, type: String, payload: JsonObject) {
        when {
            scene == WsScene.GLOBAL && type == WsType.GLOBAL_CHAT_SEND -> {
                val msg = payload["message"]?.jsonPrimitive?.content ?: return
                handleGlobalChatSend(sessionId, msg)
            }
            type == WsType.MAKE_MOVE -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                val moveObj = payload["move"]?.jsonObject ?: return
                handleMakeMove(sessionId, roomId, parseMoveDto(moveObj))
            }
            type == WsType.QUEUE_JOIN -> {
                val timeControl = payload["timeControlSeconds"]?.jsonPrimitive?.int ?: 600
                handleQueueJoin(sessionId, timeControl)
            }
            type == WsType.QUEUE_LEAVE -> handleQueueLeave(sessionId)
            type == WsType.CHAT_SEND -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                val msg = payload["message"]?.jsonPrimitive?.content ?: return
                handleChatSend(sessionId, roomId, msg)
            }
            type == WsType.RESIGN -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                handleResign(sessionId, roomId)
            }
            type == WsType.DRAW_OFFER -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                handleDrawOffer(sessionId, roomId)
            }
            type == WsType.DRAW_RESPONSE -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                val accepted = payload["accepted"]?.jsonPrimitive?.boolean ?: return
                handleDrawResponse(sessionId, roomId, accepted)
            }
            type == WsType.UNDO_REQUEST -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                handleUndoRequest(sessionId, roomId)
            }
            type == WsType.UNDO_RESPONSE -> {
                // Placeholder: undo logic can be expanded later
            }
            type == WsType.ROOM_JOIN -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                handleRoomJoin(sessionId, roomId)
            }
            type == WsType.ROOM_ABANDON -> {
                val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
                handleRoomAbandon(sessionId, roomId)
            }
            else -> log.warn("[WS] Unknown message scene={} type={}", scene, type)
        }
    }

    fun handleMakeMove(sessionId: String, roomId: String, move: MoveDto) {
        val player = sessionPlayerMap[sessionId] ?: return

        log.info("[ORCH] handleMakeMove: room={} player={} move=({},{})->({},{})",
            roomId, player.name, move.fromRow, move.fromCol, move.toRow, move.toCol)

        val result = gameService.makeMove(roomId, move, player.id)
        if (!result.success) {
            log.warn("[ORCH] handleMakeMove REJECTED: room={} player={}", roomId, player.name)
            sessionRegistry.sendToSession(sessionId, WsMessageBuilder.buildGameMessage(
                WsType.ERROR,
                ErrorPayload(code = "invalid_move", message = "Move not allowed")
            ))
            return
        }

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.MOVE_MADE,
            MoveMadePayload(roomId = roomId, move = move, playerId = player.id)
        ), excludeSessionId = sessionId)

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.TIMER_UPDATE,
            TimerUpdatePayload(roomId = roomId, redTimeMillis = result.redTimeMillis, blackTimeMillis = result.blackTimeMillis)
        ))

        when {
            result.timedOut -> {
                val timeoutResult = if (result.redTimeMillis <= 0) "black_wins" else "red_wins"
                log.info("[ORCH] Timeout in room {}: {}", roomId, timeoutResult)
                handleGameOver(roomId, timeoutResult, "timeout")
            }
            result.gameStatus != GameStatus.PLAYING -> {
                val gameOverResult = when (result.gameStatus) {
                    GameStatus.RED_WINS -> "red_wins"
                    GameStatus.BLACK_WINS -> "black_wins"
                    GameStatus.DRAW -> "draw"
                    else -> return
                }
                log.info("[ORCH] Game over by {} in room {}: {}", result.gameStatus, roomId, gameOverResult)
                handleGameOver(roomId, gameOverResult, "checkmate")
            }
            gameService.roomHasBot(roomId) -> botService.onOpponentMove(roomId, move)
        }
    }

    private fun handleGameOver(roomId: String, result: String, reason: String, grantXp: Boolean = true) {
        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.GAME_OVER,
            GameOverPayload(roomId = roomId, result = result, reason = reason)
        ))
        if (grantXp) {
            grantXpForGameOver(roomId, result)
        }

        finishRoom(roomId)
    }

    fun handleQueueJoin(sessionId: String, timeControlSeconds: Int) {
        val player = sessionPlayerMap[sessionId] ?: return
        log.info("[ORCH] handleQueueJoin: player={} timeControl={}s", player.name, timeControlSeconds)

        gameService.joinQueue(sessionId, player, timeControlSeconds)

        // Find queueing match
        val match = gameService.findMatch(sessionId)

        if (match != null) {
            log.info("[ORCH] MATCH FOUND: {} vs {}", match.first.player.name, match.second.player.name)
            val (entry1, entry2) = match
            val room = gameService.createRoom(entry1.player, "Matched Game", entry1.timeControlSeconds, false)
            gameService.joinRoom(room.id, entry2.player)

            gameService.addRoomSession(room.id, entry1.sessionId)
            gameService.addRoomSession(room.id, entry2.sessionId)

            sessionRegistry.sendToSession(entry1.sessionId, WsMessageBuilder.buildGameMessage(
                WsType.MATCH_FOUND,
                MatchFoundPayload(roomId = room.id, opponent = entry2.player, playerColor = "RED")
            ))
            sessionRegistry.sendToSession(entry2.sessionId, WsMessageBuilder.buildGameMessage(
                WsType.MATCH_FOUND,
                MatchFoundPayload(roomId = room.id, opponent = entry1.player, playerColor = "BLACK")
            ))
        } else { // if no match, then we will wait longer, and if still cannot find any match, we will assign a bot.
            val position = gameService.getQueuePosition(sessionId)
            log.info("[ORCH] No match yet for {}, queue position={}", player.name, position)
            sessionRegistry.sendToSession(sessionId, WsMessageBuilder.buildGameMessage(
                WsType.QUEUE_UPDATE,
                QueueUpdatePayload(position = position, estimatedWaitSeconds = 30)
            ))
            botService.onPlayerQueueJoin(sessionId, timeControlSeconds)
        }
    }

    fun handleQueueLeave(sessionId: String) {
        gameService.leaveQueue(sessionId)
        botService.onPlayerQueueLeave(sessionId)
    }

    fun handleChatSend(sessionId: String, roomId: String, message: String) {
        val player = sessionPlayerMap[sessionId] ?: return
        val room = gameService.getRoom(roomId) ?: return
        val isSpectator = room.spectators.containsKey(player.id)
        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.CHAT_RECEIVE,
            ChatReceivePayload(
                roomId = roomId,
                senderId = player.id,
                senderName = player.name,
                message = message,
                timestamp = System.currentTimeMillis(),
                isSpectator = isSpectator
            )
        ), excludeSessionId = sessionId)
    }

    fun handleGlobalChatSend(sessionId: String, message: String) {
        val player = sessionPlayerMap[sessionId] ?: return
        log.info("[ORCH] global_chat: player={} message={}", player.name, message)
        val globalMsg = WsMessageBuilder.buildGlobalMessage(
            WsType.GLOBAL_CHAT_RECEIVE,
            GlobalChatReceivePayload(
                senderId = player.id,
                senderName = player.name,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
        sessionRegistry.broadcastToAll(globalMsg)
    }

    fun handleResign(sessionId: String, roomId: String) {
        val player = sessionPlayerMap[sessionId] ?: return
        log.info("[ORCH] handleResign: player={} room={}", player.name, roomId)
        val result = gameService.resign(roomId, player.id) ?: return
        log.info("[ORCH] resign result: {}", result)

        handleGameOver(roomId, result, "resignation by ${player.name}")
    }

    fun handleDrawOffer(sessionId: String, roomId: String) {
        val player = sessionPlayerMap[sessionId] ?: return
        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.DRAW_OFFERED,
            DrawOfferedPayload(roomId = roomId, offeredBy = player.id)
        ), excludeSessionId = sessionId)
    }

    fun handleDrawResponse(sessionId: String, roomId: String, accepted: Boolean) {
        if (accepted) {
            if (!gameService.markAsDraw(roomId)) {
                log.warn("[ORCH] handleDrawResponse: room {} already finished, ignoring", roomId)
                return
            }
            handleGameOver(roomId, "draw", "agreement", grantXp = false)
        }
    }

    fun handleUndoRequest(sessionId: String, roomId: String) {
        val player = sessionPlayerMap[sessionId] ?: return
        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.UNDO_REQUESTED,
            UndoRequestedPayload(roomId = roomId, requestedBy = player.id)
        ), excludeSessionId = sessionId)
    }

    /**
     * Deprecated: the server now detects checkmate server-side inside makeMove().
     * Kept here only for backward compatibility during transition; it is a no-op.
     */
    @Deprecated("Client should no longer send this event to server")
    fun handleGameOverReport(sessionId: String, roomId: String) {
        log.warn("[ORCH] Received deprecated game_over_report for room {} from session {} — server handles game-over now", roomId, sessionId)
    }

    fun handleRoomJoin(sessionId: String, roomId: String) {
        val room = gameService.getRoom(roomId) ?: run {
            log.warn("[ORCH] handleRoomJoin: room {} not found", roomId)
            return
        }
        val player = sessionPlayerMap[sessionId] ?: run {
            log.warn("[ORCH] handleRoomJoin: no player for session {}", sessionId)
            return
        }
        log.info("[ORCH] handleRoomJoin: player={} room={} red={} black={}", player.name, roomId, room.redPlayer?.name, room.blackPlayer?.name)

        // Capture a consistent snapshot of all mutable room fields under the room lock.
        // Prevents races with abandonGame()/makeMove()/finishGame() which also hold synchronized(room).
        // Any room mutation (spectator add) also happens here. IO runs after the lock is released.
        data class Snapshot(
            val isRedPlayer: Boolean,
            val isBlackPlayer: Boolean,
            val status: RoomStatus,
            val isPrivate: Boolean,
            val redPlayer: Player?,
            val blackPlayer: Player?,
            val timeControlSeconds: Int,
            val moves: List<MoveDto>,
            val redTimeMillis: Long,
            val blackTimeMillis: Long,
            val spectatorAdded: Boolean,
            val gameStarted: Boolean
        )

        val snap = synchronized(room) {
            val isRedPlayer = room.redPlayer?.id == player.id
            val isBlackPlayer = room.blackPlayer?.id == player.id
            val isSpectator = !isRedPlayer && !isBlackPlayer
            val spectatorAdded = isSpectator && !room.private && room.status == RoomStatus.PLAYING
            if (spectatorAdded) room.spectators[player.id] = player
            Snapshot(
                isRedPlayer = isRedPlayer,
                isBlackPlayer = isBlackPlayer,
                status = room.status,
                isPrivate = room.private,
                redPlayer = room.redPlayer,
                blackPlayer = room.blackPlayer,
                timeControlSeconds = room.timeControlSeconds,
                moves = room.moves.toList(),
                redTimeMillis = room.redTimeMillis,
                blackTimeMillis = room.blackTimeMillis,
                spectatorAdded = spectatorAdded,
                gameStarted = room.gameStarted
            )
        }

        if (!snap.isRedPlayer && !snap.isBlackPlayer) {
            // Spectator path
            if (!snap.spectatorAdded) {
                log.warn("[ORCH] handleRoomJoin spectator REJECTED: player={} room={} private={} status={}", player.name, roomId, snap.isPrivate, snap.status)
                return
            }

            gameService.addRoomSession(roomId, sessionId)
            log.info("[ORCH] Spectator joined: player={} room={}", player.name, roomId)

            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.SPECTATOR_JOINED,
                SpectatorJoinedPayload(roomId = roomId, spectator = player)
            ), excludeSessionId = sessionId)

            val red = snap.redPlayer ?: return
            val black = snap.blackPlayer ?: return
            sessionRegistry.sendToSession(sessionId, WsMessageBuilder.buildGameMessage(
                WsType.GAME_STATE,
                GameStatePayload(
                    roomId = roomId,
                    redPlayer = red,
                    blackPlayer = black,
                    timeControlSeconds = snap.timeControlSeconds,
                    moves = snap.moves,
                    redTimeMillis = snap.redTimeMillis,
                    blackTimeMillis = snap.blackTimeMillis
                )
            ))
            return
        }

        // Player path (red or black reconnect / initial join)
        gameService.addRoomSession(roomId, sessionId)
        log.info("[ORCH] Room {} sessions: {}", roomId, gameService.getRoomSessionIds(roomId).size)

        if (snap.gameStarted) {
            // Reconnect branch: game already started — send full state and notify opponent
            log.info("[ORCH] Player {} reconnected to in-progress room {}", player.name, roomId)
            val red = snap.redPlayer ?: return
            val black = snap.blackPlayer ?: return
            sessionRegistry.sendToSession(sessionId, WsMessageBuilder.buildGameMessage(
                WsType.GAME_STATE,
                GameStatePayload(
                    roomId = roomId,
                    redPlayer = red,
                    blackPlayer = black,
                    timeControlSeconds = snap.timeControlSeconds,
                    moves = snap.moves,
                    redTimeMillis = snap.redTimeMillis,
                    blackTimeMillis = snap.blackTimeMillis
                )
            ))
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.OPPONENT_RECONNECTED,
                OpponentReconnectedPayload(roomId = roomId)
            ), excludeSessionId = sessionId)
            if (gameService.roomHasBot(roomId)) {
                botService.resumeBotGame(roomId)
            }
        } else {
            // First-join branch
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.OPPONENT_JOINED,
                OpponentJoinedPayload(roomId = roomId, opponent = player)
            ), excludeSessionId = sessionId)

            if (snap.status == RoomStatus.PLAYING && snap.redPlayer != null && snap.blackPlayer != null) {
                val justStarted = gameService.recordGameStart(roomId)
                if (justStarted) {
                    log.info("[ORCH] Both players in room {}, broadcasting game_started", roomId)
                    broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                        WsType.GAME_STARTED,
                        GameStartedPayload(
                            roomId = roomId,
                            redPlayer = snap.redPlayer,
                            blackPlayer = snap.blackPlayer,
                            timeControlSeconds = snap.timeControlSeconds
                        )
                    ))
                }
            }
        }
    }

    fun handleRoomAbandon(sessionId: String, roomId: String) {
        val player = sessionPlayerMap[sessionId] ?: return
        val abandonGameResult = gameService.abandonGame(roomId, player.id) ?: return

        val (room, forfeitResult) = abandonGameResult
        handleGameOver(roomId, forfeitResult, "abandonment")
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    fun broadcastToRoom(roomId: String, message: String, excludeSessionId: String? = null) {
        gameService.getRoomSessionIds(roomId).forEach { sessionId ->
            if (sessionId != excludeSessionId) {
                sessionRegistry.sendToSession(sessionId, message)
            }
        }
    }

    /**
     * Central cleanup for a finished game. Must be called AFTER grantXpForGameOver()
     * because XP granting needs the room to still exist for roomHasBot() lookup.
     */
    private fun finishRoom(roomId: String) {
        botService.onGameOver(roomId)
        gameService.removeRoom(roomId)
    }

    private fun grantXpForGameOver(roomId: String, result: String) {
        val room = gameService.getRoom(roomId) ?: return
        val isVsBot = gameService.roomHasBot(roomId)
        val xpGain = if (isVsBot) 30 else 50

        val winnerId = when (result) {
            "red_wins" -> room.redPlayer?.id
            "black_wins" -> room.blackPlayer?.id
            else -> null // draw
        }

        if (winnerId == null || winnerId.startsWith("bot-")) return

        val winner = persistenceService.persistXpGain(winnerId, xpGain)
        val winnerSessionId = winner?.let {
            sessionPlayerMap
                .filterValues { it.id == winnerId }
                .keys
                .firstOrNull()
        }
        if (winner == null || winnerSessionId == null) return

        sessionPlayerMap.computeIfPresent(winnerSessionId) { _, _ -> winner }
        sessionRegistry.sendToSession(winnerSessionId, WsMessageBuilder.buildGameMessage(
            WsType.XP_UPDATE,
            XpUpdatePayload(newXp = winner.xp, newLevel = winner.level, xpGained = xpGain)
        ))


        log.info("[ORCH] XP granted: player={} +{}xp newXp={} newLevel={}", winner.name, xpGain, winner.xp, winner.level)
    }
}



private fun parseMoveDto(obj: JsonObject): MoveDto = MoveDto(
    fromRow = obj["fromRow"]!!.jsonPrimitive.int,
    fromCol = obj["fromCol"]!!.jsonPrimitive.int,
    toRow = obj["toRow"]!!.jsonPrimitive.int,
    toCol = obj["toCol"]!!.jsonPrimitive.int
)
