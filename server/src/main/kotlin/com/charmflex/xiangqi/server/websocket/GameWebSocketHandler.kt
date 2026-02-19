package com.charmflex.xiangqi.server.websocket

import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.service.BotService
import com.charmflex.xiangqi.server.service.GameService
import com.charmflex.xiangqi.server.service.JwtValidator
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Component
class GameWebSocketHandler(
    private val gameService: GameService,
    private val botService: BotService,
    private val jwtValidator: JwtValidator
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(GameWebSocketHandler::class.java)

    // All active WebSocket sessions: sessionId -> WebSocketSession
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    // roomId -> set of sessionIds (players + spectators)
    private val roomSessions = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        botService.messageSender = { target, message, excludeSessionId ->
            if (sessions.containsKey(target)) {
                sendToSession(target, message)
            } else {
                broadcastToRoom(target, message, excludeSessionId)
            }
        }
        botService.roomSessionRegistrar = { roomId, sessionId ->
            addRoomSession(roomId, sessionId)
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("[WS] Connection established: sessionId={} uri={}", session.id, session.uri)
        sessions[session.id] = session
        val params = session.uri?.query?.split("&")?.mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }?.toMap() ?: emptyMap()

        val token = params["token"]
        val name = params["name"]
        log.info("[WS] Connection params: token={} name={}", token?.take(8)?.plus("..."), name)

        when {
            token != null -> {
                if (jwtValidator.isJwtToken(token)) {
                    val userId = jwtValidator.validateAndGetUserId(token)
                    if (userId != null) {
                        val player = gameService.getPlayerById(userId)
                        if (player != null) {
                            gameService.registerPlayerByToken(session.id, userId)
                            log.info("[WS] JWT auth: linked session {} to player id={} name={}", session.id, player.id.take(8), player.name)
                        } else {
                            val fallbackName = name ?: "Player_${userId.take(6)}"
                            log.warn("[WS] JWT auth: player {} not found, creating: {}", userId.take(8), fallbackName)
                            val newPlayer = gameService.getOrCreatePlayer(userId, fallbackName)
                            gameService.registerPlayerByToken(session.id, newPlayer.id)
                        }
                    } else {
                        val fallbackName = name ?: "Guest_${session.id.take(6)}"
                        log.warn("[WS] JWT validation failed, registering as: {}", fallbackName)
                        gameService.registerPlayer(session.id, fallbackName)
                    }
                } else {
                    val player = gameService.registerPlayerByToken(session.id, token)
                    if (player != null) {
                        log.info("[WS] Linked session {} to existing player: id={} name={}", session.id, player.id.take(8), player.name)
                    } else {
                        val fallbackName = name ?: "Guest_${session.id.take(6)}"
                        log.warn("[WS] Token {} not found, registering as: {}", token.take(8), fallbackName)
                        gameService.registerPlayer(session.id, fallbackName)
                    }
                }
            }
            name != null -> {
                log.info("[WS] Registering by name: {}", name)
                gameService.registerPlayer(session.id, name)
            }
            else -> {
                val defaultName = "Guest_${session.id.take(6)}"
                log.info("[WS] No token/name, registering as: {}", defaultName)
                gameService.registerPlayer(session.id, defaultName)
            }
        }
        log.info("[WS] Total active sessions: {}", sessions.size)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info("[WS] Connection closed: sessionId={} status={}", session.id, status)
        sessions.remove(session.id)
        botService.onPlayerQueueLeave(session.id)
        gameService.removeSession(session.id)
        roomSessions.forEach { (roomId, sessionIds) ->
            if (sessionIds.remove(session.id)) {
                log.info("[WS] Notifying room {} about disconnection of session {}", roomId, session.id)
                broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                    WsType.OPPONENT_DISCONNECTED,
                    OpponentDisconnectedPayload(roomId = roomId)
                ), excludeSessionId = session.id)
                if (botService.hasBot(roomId)) {
                    botService.onGameOver(roomId)
                }
            }
        }
        log.info("[WS] Remaining sessions: {}", sessions.size)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.info("[WS] <<< RECV from session {}: {}", session.id, message.payload)
        try {
            val envelope = json.decodeFromString<JsonObject>(message.payload)
            val scene = envelope["scene"]?.jsonPrimitive?.content ?: WsScene.GAME
            val type = envelope["type"]?.jsonPrimitive?.content ?: return
            val payload = envelope["payload"]?.jsonObject ?: return
            log.info("[WS] Message scene={} type={} from session={}", scene, type, session.id)

            when {
                scene == WsScene.GLOBAL && type == WsType.GLOBAL_CHAT_SEND -> handleGlobalChatSend(session, payload)
                type == WsType.MAKE_MOVE -> handleMakeMove(session, payload)
                type == WsType.QUEUE_JOIN -> handleQueueJoin(session, payload)
                type == WsType.QUEUE_LEAVE -> handleQueueLeave(session)
                type == WsType.CHAT_SEND -> handleChatSend(session, payload)
                type == WsType.RESIGN -> handleResign(session, payload)
                type == WsType.DRAW_OFFER -> handleDrawOffer(session, payload)
                type == WsType.DRAW_RESPONSE -> handleDrawResponse(session, payload)
                type == WsType.UNDO_REQUEST -> handleUndoRequest(session, payload)
                type == WsType.UNDO_RESPONSE -> handleUndoResponse(session, payload)
                type == WsType.GAME_OVER_REPORT -> handleGameOverReport(session, payload)
                type == WsType.ROOM_JOIN -> handleRoomJoin(session, payload)
                else -> log.warn("[WS] Unknown message scene={} type={}", scene, type)
            }
        } catch (e: Exception) {
            log.error("[WS] Error handling message from session {}: {}", session.id, e.message, e)
            sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                WsType.ERROR,
                ErrorPayload(code = "parse_error", message = e.message ?: "Invalid message")
            ))
        }
    }

    private fun handleMakeMove(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val moveObj = payload["move"]?.jsonObject ?: return
        val move = MoveDto(
            fromRow = moveObj["fromRow"]!!.jsonPrimitive.int,
            fromCol = moveObj["fromCol"]!!.jsonPrimitive.int,
            toRow = moveObj["toRow"]!!.jsonPrimitive.int,
            toCol = moveObj["toCol"]!!.jsonPrimitive.int
        )

        val player = gameService.getPlayerBySession(session.id) ?: run {
            log.warn("[WS] make_move: no player for session {}", session.id)
            return
        }
        log.info("[WS] make_move: room={} player={} move=({},{})->({},{})", roomId, player.name, move.fromRow, move.fromCol, move.toRow, move.toCol)
        val success = gameService.makeMove(roomId, move, player.id)
        if (!success) {
            log.warn("[WS] make_move REJECTED: room={} player={}", roomId, player.name)
            sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                WsType.ERROR,
                ErrorPayload(code = "invalid_move", message = "Move not allowed")
            ))
            return
        }
        log.info("[WS] make_move OK, broadcasting to room {}", roomId)

        val room = gameService.getRoom(roomId) ?: return

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.MOVE_MADE,
            MoveMadePayload(roomId = roomId, move = move, playerId = player.id)
        ), excludeSessionId = session.id)

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.TIMER_UPDATE,
            TimerUpdatePayload(roomId = roomId, redTimeMillis = room.redTimeMillis, blackTimeMillis = room.blackTimeMillis)
        ))

        if (room.status == RoomStatus.FINISHED) {
            val result = if (room.redTimeMillis <= 0) "black_wins" else "red_wins"
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.GAME_OVER,
                GameOverPayload(roomId = roomId, result = result, reason = "timeout")
            ))
            botService.onGameOver(roomId)
        } else if (botService.hasBot(roomId)) {
            botService.onOpponentMove(roomId, move)
        }
    }

    private fun handleQueueJoin(session: WebSocketSession, payload: JsonObject) {
        val player = gameService.getPlayerBySession(session.id) ?: run {
            log.warn("[WS] queue_join: no player for session {}", session.id)
            return
        }
        val timeControl = payload["timeControlSeconds"]?.jsonPrimitive?.int ?: 600
        log.info("[WS] queue_join: player={} timeControl={}s", player.name, timeControl)

        gameService.joinQueue(session.id, player, timeControl)

        val match = gameService.findMatch(session.id)
        if (match != null) {
            log.info("[WS] MATCH FOUND: {} vs {}", match.first.player.name, match.second.player.name)
            val (entry1, entry2) = match
            val room = gameService.createRoom(entry1.player, "Matched Game", entry1.timeControlSeconds, false)
            gameService.joinRoom(room.id, entry2.player)

            roomSessions.getOrPut(room.id) { ConcurrentHashMap.newKeySet() }.apply {
                add(entry1.sessionId)
                add(entry2.sessionId)
            }

            sendToSession(entry1.sessionId, WsMessageBuilder.buildGameMessage(
                WsType.MATCH_FOUND,
                MatchFoundPayload(roomId = room.id, opponent = entry2.player, playerColor = "RED")
            ))
            sendToSession(entry2.sessionId, WsMessageBuilder.buildGameMessage(
                WsType.MATCH_FOUND,
                MatchFoundPayload(roomId = room.id, opponent = entry1.player, playerColor = "BLACK")
            ))
        } else {
            val position = gameService.getQueuePosition(session.id)
            log.info("[WS] No match yet for {}, queue position={}", player.name, position)
            sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                WsType.QUEUE_UPDATE,
                QueueUpdatePayload(position = position, estimatedWaitSeconds = 30)
            ))
            botService.onPlayerQueueJoin(session.id, timeControl)
        }
    }

    private fun handleQueueLeave(session: WebSocketSession) {
        gameService.leaveQueue(session.id)
        botService.onPlayerQueueLeave(session.id)
    }

    private fun handleChatSend(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val message = payload["message"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return
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
        ), excludeSessionId = session.id)
    }

    private fun handleGlobalChatSend(session: WebSocketSession, payload: JsonObject) {
        val message = payload["message"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return
        log.info("[WS] global_chat: player={} message={}", player.name, message)

        val globalMsg = WsMessageBuilder.buildGlobalMessage(
            WsType.GLOBAL_CHAT_RECEIVE,
            GlobalChatReceivePayload(
                senderId = player.id,
                senderName = player.name,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
        // Broadcast to all connected sessions
        sessions.keys.forEach { sessionId -> sendToSession(sessionId, globalMsg) }
    }

    private fun handleResign(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return
        log.info("[WS] resign: player={} room={}", player.name, roomId)
        val result = gameService.resign(roomId, player.id) ?: return
        log.info("[WS] resign result: {}", result)

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.GAME_OVER,
            GameOverPayload(roomId = roomId, result = result, reason = "resignation")
        ))
        botService.onGameOver(roomId)

        // Award XP to winner
        grantXpForGameOver(roomId, result)
    }

    private fun handleDrawOffer(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.DRAW_OFFERED,
            DrawOfferedPayload(roomId = roomId, offeredBy = player.id)
        ), excludeSessionId = session.id)
    }

    private fun handleDrawResponse(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val accepted = payload["accepted"]?.jsonPrimitive?.boolean ?: return

        if (accepted) {
            val room = gameService.getRoom(roomId) ?: return
            room.status = RoomStatus.FINISHED
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.GAME_OVER,
                GameOverPayload(roomId = roomId, result = "draw", reason = "agreement")
            ))
            botService.onGameOver(roomId)
            // No XP for draws
        }
    }

    private fun handleUndoRequest(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.UNDO_REQUESTED,
            UndoRequestedPayload(roomId = roomId, requestedBy = player.id)
        ), excludeSessionId = session.id)
    }

    private fun handleUndoResponse(session: WebSocketSession, payload: JsonObject) {
        // Placeholder: undo logic can be expanded later
        @Suppress("UNUSED_VARIABLE")
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
    }

    private fun handleRoomJoin(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val room = gameService.getRoom(roomId) ?: run {
            log.warn("[WS] room_join: room {} not found", roomId)
            return
        }
        val player = gameService.getPlayerBySession(session.id) ?: run {
            log.warn("[WS] room_join: no player for session {}", session.id)
            return
        }
        log.info("[WS] room_join: player={} room={} red={} black={}", player.name, roomId, room.redPlayer?.name, room.blackPlayer?.name)

        val isRedPlayer = room.redPlayer?.id == player.id
        val isBlackPlayer = room.blackPlayer?.id == player.id

        if (!isRedPlayer && !isBlackPlayer) {
            // Player is not red or black - join as spectator if room is public and playing
            if (room.private || room.status != RoomStatus.PLAYING) {
                log.warn("[WS] room_join spectator REJECTED: player={} room={} private={} status={}", player.name, roomId, room.private, room.status)
                return
            }

            room.spectators[player.id] = player
            roomSessions.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(session.id)
            log.info("[WS] Spectator joined: player={} room={}", player.name, roomId)

            // Notify existing room members about the new spectator
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.SPECTATOR_JOINED,
                SpectatorJoinedPayload(roomId = roomId, spectator = player)
            ), excludeSessionId = session.id)

            // Send current game state to spectator
            val red = room.redPlayer ?: return
            val black = room.blackPlayer ?: return
            sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                WsType.GAME_STATE,
                GameStatePayload(
                    roomId = roomId,
                    redPlayer = red,
                    blackPlayer = black,
                    timeControlSeconds = room.timeControlSeconds,
                    moves = room.moves.toList(),
                    redTimeMillis = room.redTimeMillis,
                    blackTimeMillis = room.blackTimeMillis
                )
            ))
            return
        }

        roomSessions.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(session.id)
        log.info("[WS] Room {} sessions: {}", roomId, roomSessions[roomId]?.size ?: 0)

        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.OPPONENT_JOINED,
            OpponentJoinedPayload(roomId = roomId, opponent = player)
        ), excludeSessionId = session.id)

        if (room.status == RoomStatus.PLAYING && room.redPlayer != null && room.blackPlayer != null) {
            log.info("[WS] Both players in room {}, broadcasting game_started", roomId)
            room.lastMoveTimestamp = System.currentTimeMillis()
            broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
                WsType.GAME_STARTED,
                GameStartedPayload(
                    roomId = roomId,
                    redPlayer = room.redPlayer!!,
                    blackPlayer = room.blackPlayer!!,
                    timeControlSeconds = room.timeControlSeconds
                )
            ))
        }
    }

    private fun handleGameOverReport(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val result = payload["result"]?.jsonPrimitive?.content ?: return
        val reason = payload["reason"]?.jsonPrimitive?.content ?: "checkmate"
        log.info("[WS] game_over_report: room={} result={} reason={}", roomId, result, reason)

        val room = gameService.getRoom(roomId) ?: return
        if (room.status == RoomStatus.FINISHED) {
            log.info("[WS] Room {} already finished, ignoring", roomId)
            return
        }

        room.status = RoomStatus.FINISHED
        broadcastToRoom(roomId, WsMessageBuilder.buildGameMessage(
            WsType.GAME_OVER,
            GameOverPayload(roomId = roomId, result = result, reason = reason)
        ))
        botService.onGameOver(roomId)

        // Award XP to winner
        grantXpForGameOver(roomId, result)
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

        val updatedPlayer = gameService.addXp(winnerId, xpGain)
        val winnerSessionId = gameService.getSessionIdForPlayer(winnerId) ?: return

        sendToSession(winnerSessionId, WsMessageBuilder.buildGameMessage(
            WsType.XP_UPDATE,
            XpUpdatePayload(newXp = updatedPlayer.xp, newLevel = updatedPlayer.level, xpGained = xpGain)
        ))
        log.info("[WS] XP granted: player={} +{}xp newXp={} newLevel={}", updatedPlayer.name, xpGain, updatedPlayer.xp, updatedPlayer.level)
    }

    fun sendToSession(sessionId: String, message: String) {
        sessions[sessionId]?.let { s ->
            if (s.isOpen) {
                log.info("[WS] >>> SEND to session {}: {}", sessionId, message)
                s.sendMessage(TextMessage(message))
            } else {
                log.warn("[WS] Session {} is closed, message dropped", sessionId)
            }
        } ?: run {
            if (!gameService.isBotSession(sessionId)) {
                log.warn("[WS] Session {} not found, message dropped", sessionId)
            }
        }
    }

    fun broadcastToRoom(roomId: String, message: String, excludeSessionId: String? = null) {
        val sessionIds = roomSessions[roomId]
        log.info("[WS] Broadcasting to room {} ({} sessions, exclude={})", roomId, sessionIds?.size ?: 0, excludeSessionId)
        sessionIds?.forEach { sessionId ->
            if (sessionId != excludeSessionId) {
                sendToSession(sessionId, message)
            }
        }
    }

    fun addRoomSession(roomId: String, sessionId: String) {
        roomSessions.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(sessionId)
    }
}
