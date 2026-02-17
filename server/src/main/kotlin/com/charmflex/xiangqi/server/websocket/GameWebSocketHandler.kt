package com.charmflex.xiangqi.server.websocket

import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.service.BotService
import com.charmflex.xiangqi.server.service.GameService
import com.charmflex.xiangqi.server.service.JwtValidator
import kotlinx.serialization.encodeToString
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

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    // roomId -> set of sessionIds
    private val roomSessions = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        // Wire up BotService callbacks so it can send messages to real players
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
                    // JWT token from Supabase auth - validate and link
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
                    // UUID token from guest auth
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
                broadcastToRoom(roomId, buildEnvelope("opponent_disconnected", buildJsonObject {
                    put("roomId", roomId)
                }), excludeSessionId = session.id)
                // Notify bot service about disconnection
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
            val type = envelope["type"]?.jsonPrimitive?.content ?: return
            val payload = envelope["payload"]?.jsonObject ?: return
            log.info("[WS] Message type={} from session={}", type, session.id)

            when (type) {
                "make_move" -> handleMakeMove(session, payload)
                "queue_join" -> handleQueueJoin(session, payload)
                "queue_leave" -> handleQueueLeave(session)
                "chat_send" -> handleChatSend(session, payload)
                "resign" -> handleResign(session, payload)
                "draw_offer" -> handleDrawOffer(session, payload)
                "draw_response" -> handleDrawResponse(session, payload)
                "undo_request" -> handleUndoRequest(session, payload)
                "undo_response" -> handleUndoResponse(session, payload)
                "game_over_report" -> handleGameOverReport(session, payload)
                "room_join" -> handleRoomJoin(session, payload)
                else -> log.warn("[WS] Unknown message type: {}", type)
            }
        } catch (e: Exception) {
            log.error("[WS] Error handling message from session {}: {}", session.id, e.message, e)
            sendToSession(session.id, buildEnvelope("error", buildJsonObject {
                put("code", "parse_error")
                put("message", e.message ?: "Invalid message")
            }))
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
            sendToSession(session.id, buildEnvelope("error", buildJsonObject {
                put("code", "invalid_move")
                put("message", "Move not allowed")
            }))
            return
        }
        log.info("[WS] make_move OK, broadcasting to room {}", roomId)

        val room = gameService.getRoom(roomId) ?: return

        broadcastToRoom(roomId, buildEnvelope("move_made", buildJsonObject {
            put("roomId", roomId)
            put("move", buildJsonObject {
                put("fromRow", move.fromRow)
                put("fromCol", move.fromCol)
                put("toRow", move.toRow)
                put("toCol", move.toCol)
            })
            put("playerId", player.id)
        }), excludeSessionId = session.id)

        broadcastToRoom(roomId, buildEnvelope("timer_update", buildJsonObject {
            put("roomId", roomId)
            put("redTimeMillis", room.redTimeMillis)
            put("blackTimeMillis", room.blackTimeMillis)
        }))

        if (room.status == RoomStatus.FINISHED) {
            val result = if (room.redTimeMillis <= 0) "black_wins" else "red_wins"
            broadcastToRoom(roomId, buildEnvelope("game_over", buildJsonObject {
                put("roomId", roomId)
                put("result", result)
                put("reason", "timeout")
            }))
            botService.onGameOver(roomId)
        } else if (botService.hasBot(roomId)) {
            // Notify bot about the human's move so it can respond
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

        // Try to find a real match first
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

            sendToSession(entry1.sessionId, buildEnvelope("match_found", buildJsonObject {
                put("roomId", room.id)
                put("opponent", buildJsonObject {
                    put("id", entry2.player.id)
                    put("name", entry2.player.name)
                    put("rating", entry2.player.rating)
                })
                put("playerColor", "RED")
            }))

            sendToSession(entry2.sessionId, buildEnvelope("match_found", buildJsonObject {
                put("roomId", room.id)
                put("opponent", buildJsonObject {
                    put("id", entry1.player.id)
                    put("name", entry1.player.name)
                    put("rating", entry1.player.rating)
                })
                put("playerColor", "BLACK")
            }))
        } else {
            val position = gameService.getQueuePosition(session.id)
            log.info("[WS] No match yet for {}, queue position={}", player.name, position)
            sendToSession(session.id, buildEnvelope("queue_update", buildJsonObject {
                put("position", position)
                put("estimatedWaitSeconds", 30)
            }))

            // Start bot matchmaking timer (will create a bot opponent if no real match within 10-15s)
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

        broadcastToRoom(roomId, buildEnvelope("chat_receive", buildJsonObject {
            put("roomId", roomId)
            put("senderId", player.id)
            put("senderName", player.name)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }), excludeSessionId = session.id)
    }

    private fun handleResign(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return
        log.info("[WS] resign: player={} room={}", player.name, roomId)
        val result = gameService.resign(roomId, player.id) ?: return
        log.info("[WS] resign result: {}", result)

        broadcastToRoom(roomId, buildEnvelope("game_over", buildJsonObject {
            put("roomId", roomId)
            put("result", result)
            put("reason", "resignation")
        }))
        botService.onGameOver(roomId)
    }

    private fun handleDrawOffer(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return

        broadcastToRoom(roomId, buildEnvelope("draw_offered", buildJsonObject {
            put("roomId", roomId)
            put("offeredBy", player.id)
        }), excludeSessionId = session.id)
    }

    private fun handleDrawResponse(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val accepted = payload["accepted"]?.jsonPrimitive?.boolean ?: return

        if (accepted) {
            val room = gameService.getRoom(roomId) ?: return
            room.status = RoomStatus.FINISHED
            broadcastToRoom(roomId, buildEnvelope("game_over", buildJsonObject {
                put("roomId", roomId)
                put("result", "draw")
                put("reason", "agreement")
            }))
            botService.onGameOver(roomId)
        }
    }

    private fun handleUndoRequest(session: WebSocketSession, payload: JsonObject) {
        val roomId = payload["roomId"]?.jsonPrimitive?.content ?: return
        val player = gameService.getPlayerBySession(session.id) ?: return

        broadcastToRoom(roomId, buildEnvelope("undo_requested", buildJsonObject {
            put("roomId", roomId)
            put("requestedBy", player.id)
        }), excludeSessionId = session.id)
    }

    private fun handleUndoResponse(session: WebSocketSession, payload: JsonObject) {
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

        if (room.redPlayer?.id != player.id && room.blackPlayer?.id != player.id) {
            log.warn("[WS] room_join REJECTED: player {} not in room {}", player.name, roomId)
            return
        }

        roomSessions.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(session.id)
        log.info("[WS] Room {} sessions: {}", roomId, roomSessions[roomId]?.size ?: 0)

        broadcastToRoom(roomId, buildEnvelope("opponent_joined", buildJsonObject {
            put("roomId", roomId)
            put("opponent", buildJsonObject {
                put("id", player.id)
                put("name", player.name)
                put("rating", player.rating)
            })
        }), excludeSessionId = session.id)

        if (room.status == RoomStatus.PLAYING && room.redPlayer != null && room.blackPlayer != null) {
            log.info("[WS] Both players in room {}, broadcasting game_started", roomId)
            room.lastMoveTimestamp = System.currentTimeMillis()

            val red = room.redPlayer!!
            val black = room.blackPlayer!!
            broadcastToRoom(roomId, buildEnvelope("game_started", buildJsonObject {
                put("roomId", roomId)
                put("redPlayer", buildJsonObject {
                    put("id", red.id)
                    put("name", red.name)
                    put("rating", red.rating)
                })
                put("blackPlayer", buildJsonObject {
                    put("id", black.id)
                    put("name", black.name)
                    put("rating", black.rating)
                })
                put("timeControlSeconds", room.timeControlSeconds)
            }))
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
        broadcastToRoom(roomId, buildEnvelope("game_over", buildJsonObject {
            put("roomId", roomId)
            put("result", result)
            put("reason", reason)
        }))
        botService.onGameOver(roomId)
    }

    private fun buildEnvelope(type: String, payload: JsonObject): String {
        return json.encodeToString(buildJsonObject {
            put("type", type)
            put("payload", payload)
        })
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
            // Session not found - could be a bot session, which is expected
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

    // Called by BotService to register a session for a room (for bot-matched games)
    fun addRoomSession(roomId: String, sessionId: String) {
        roomSessions.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(sessionId)
    }
}
