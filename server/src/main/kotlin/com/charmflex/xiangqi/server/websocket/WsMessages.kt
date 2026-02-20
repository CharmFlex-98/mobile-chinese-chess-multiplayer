package com.charmflex.xiangqi.server.websocket

import com.charmflex.xiangqi.server.model.MoveDto
import com.charmflex.xiangqi.server.model.Player
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

val wsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object WsScene {
    const val GAME = "game"
    const val GLOBAL = "global"
}

object WsType {
    // Game scene - server to client
    const val MOVE_MADE = "move_made"
    const val GAME_STARTED = "game_started"
    const val GAME_OVER = "game_over"
    const val GAME_STATE = "game_state"
    const val QUEUE_UPDATE = "queue_update"
    const val MATCH_FOUND = "match_found"
    const val CHAT_RECEIVE = "chat_receive"
    const val TIMER_UPDATE = "timer_update"
    const val OPPONENT_JOINED = "opponent_joined"
    const val OPPONENT_DISCONNECTED = "opponent_disconnected"
    const val OPPONENT_RECONNECTED = "opponent_reconnected"
    const val DRAW_OFFERED = "draw_offered"
    const val UNDO_REQUESTED = "undo_requested"
    const val SPECTATOR_JOINED = "spectator_joined"
    const val SPECTATOR_LEFT = "spectator_left"
    const val XP_UPDATE = "xp_update"
    const val ERROR = "error"

    // Global scene - server to client
    const val GLOBAL_CHAT_RECEIVE = "global_chat_receive"

    // Game scene - client to server
    const val MAKE_MOVE = "make_move"
    const val QUEUE_JOIN = "queue_join"
    const val QUEUE_LEAVE = "queue_leave"
    const val CHAT_SEND = "chat_send"
    const val RESIGN = "resign"
    const val DRAW_OFFER = "draw_offer"
    const val DRAW_RESPONSE = "draw_response"
    const val UNDO_REQUEST = "undo_request"
    const val UNDO_RESPONSE = "undo_response"
    const val ROOM_JOIN = "room_join"
    const val ROOM_ABANDON = "room_abandon"
    const val GAME_OVER_REPORT = "game_over_report"

    // Global scene - client to server
    const val GLOBAL_CHAT_SEND = "global_chat_send"
}

// ---- Server-to-client payload data classes ----

@Serializable
data class MoveMadePayload(val roomId: String, val move: MoveDto, val playerId: String)

@Serializable
data class GameStartedPayload(
    val roomId: String,
    val redPlayer: Player,
    val blackPlayer: Player,
    val timeControlSeconds: Int
)

@Serializable
data class GameStatePayload(
    val roomId: String,
    val redPlayer: Player,
    val blackPlayer: Player,
    val timeControlSeconds: Int,
    val moves: List<MoveDto>,
    val redTimeMillis: Long,
    val blackTimeMillis: Long
)

@Serializable
data class GameOverPayload(val roomId: String, val result: String, val reason: String)

@Serializable
data class QueueUpdatePayload(val position: Int, val estimatedWaitSeconds: Int)

@Serializable
data class MatchFoundPayload(val roomId: String, val opponent: Player, val playerColor: String)

@Serializable
data class ChatReceivePayload(
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isSpectator: Boolean = false
)

@Serializable
data class GlobalChatReceivePayload(
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long
)

@Serializable
data class TimerUpdatePayload(val roomId: String, val redTimeMillis: Long, val blackTimeMillis: Long)

@Serializable
data class OpponentJoinedPayload(val roomId: String, val opponent: Player)

@Serializable
data class OpponentDisconnectedPayload(val roomId: String)

@Serializable
data class DrawOfferedPayload(val roomId: String, val offeredBy: String)

@Serializable
data class UndoRequestedPayload(val roomId: String, val requestedBy: String)

@Serializable
data class SpectatorJoinedPayload(val roomId: String, val spectator: Player)

@Serializable
data class SpectatorLeftPayload(val roomId: String, val spectatorId: String)

@Serializable
data class XpUpdatePayload(val newXp: Int, val newLevel: Int, val xpGained: Int)

@Serializable
data class ErrorPayload(val code: String, val message: String)

// ---- Message builder ----

object WsMessageBuilder {
    inline fun <reified T> buildMessage(type: String, scene: String, payload: T): String {
        val payloadElement = wsJson.encodeToJsonElement(payload)
        val envelope = buildJsonObject {
            put("type", type)
            put("scene", scene)
            put("payload", payloadElement)
        }
        return wsJson.encodeToString(envelope)
    }

    inline fun <reified T> buildGameMessage(type: String, payload: T): String =
        buildMessage(type, WsScene.GAME, payload)

    inline fun <reified T> buildGlobalMessage(type: String, payload: T): String =
        buildMessage(type, WsScene.GLOBAL, payload)
}
