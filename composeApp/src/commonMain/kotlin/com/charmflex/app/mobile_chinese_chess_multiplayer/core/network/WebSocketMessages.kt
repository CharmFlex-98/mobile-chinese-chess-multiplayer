package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.model.MoveDto
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.profile.Player
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class WsEnvelope(
    val scene: String,
    val type: String,
    val payload: JsonElement
)

@Serializable
sealed interface WsClientMessage {
    val scene: String
    val type: String
}

interface WsServerMessage

val wsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

// ---- Game scene client messages ----

@Serializable
sealed class GameClientMessage : WsClientMessage {
    override val scene: String = "game"
}

@Serializable
@SerialName("make_move")
data class MakeMove(
    val roomId: String,
    val move: MoveDto
) : GameClientMessage() {
    override val type: String get() = "make_move"
}

@Serializable
@SerialName("queue_join")
data class QueueJoin(
    val timeControlSeconds: Int = 600
) : GameClientMessage() {
    override val type: String get() = "queue_join"
}

@Serializable
@SerialName("queue_leave")
data object QueueLeave : GameClientMessage() {
    override val type: String get() = "queue_leave"
}

@Serializable
@SerialName("chat_send")
data class ChatSend(
    val roomId: String,
    val message: String
) : GameClientMessage() {
    override val type: String get() = "chat_send"
}

@Serializable
@SerialName("resign")
data class Resign(
    val roomId: String
) : GameClientMessage() {
    override val type: String get() = "resign"
}

@Serializable
@SerialName("draw_offer")
data class DrawOffer(
    val roomId: String
) : GameClientMessage() {
    override val type: String get() = "draw_offer"
}

@Serializable
@SerialName("draw_response")
data class DrawResponse(
    val roomId: String,
    val accepted: Boolean
) : GameClientMessage() {
    override val type: String get() = "draw_response"
}

@Serializable
@SerialName("undo_request")
data class UndoRequest(
    val roomId: String
) : GameClientMessage() {
    override val type: String get() = "undo_request"
}

@Serializable
@SerialName("undo_response")
data class UndoResponse(
    val roomId: String,
    val accepted: Boolean
) : GameClientMessage() {
    override val type: String get() = "undo_response"
}

@Serializable
@SerialName("room_join")
data class RoomJoin(
    val roomId: String
) : GameClientMessage() {
    override val type: String get() = "room_join"
}

@Serializable
@SerialName("room_abandon")
data class RoomAbandon(
    val roomId: String
) : GameClientMessage() {
    override val type: String get() = "room_abandon"
}

@Serializable
@SerialName("game_over_report")
data class GameClientOverReport(
    val roomId: String,
    val result: String,
    val reason: String
) : GameClientMessage() {
    override val type: String get() = "game_over_report"
}

// ---- Global scene client messages ----

@Serializable
sealed class GlobalClientMessage : WsClientMessage {
    override val scene: String = "global"
}

@Serializable
@SerialName("global_chat_send")
data class GlobalChatSend(
    val message: String
) : GlobalClientMessage() {
    override val type: String get() = "global_chat_send"
}

// ---- Server-to-client messages (game scene) ----

@Serializable
@SerialName("move_made")
data class MoveMade(
    val roomId: String,
    val move: MoveDto,
    val playerId: String
) : WsServerMessage

@Serializable
@SerialName("game_started")
data class GameStarted(
    val roomId: String,
    val redPlayer: Player,
    val blackPlayer: Player,
    val timeControlSeconds: Int
) : WsServerMessage

@Serializable
@SerialName("game_state")
data class RoomSnapshot(
    val roomId: String,
    val redPlayer: Player,
    val blackPlayer: Player,
    val timeControlSeconds: Int,
    val moves: List<MoveDto>,
    val redTimeMillis: Long,
    val blackTimeMillis: Long
) : WsServerMessage

@Serializable
@SerialName("game_over")
data class GameOver(
    val roomId: String,
    val result: String,
    val reason: String
) : WsServerMessage

@Serializable
@SerialName("queue_update")
data class QueueUpdate(
    val position: Int,
    val estimatedWaitSeconds: Int
) : WsServerMessage

@Serializable
@SerialName("match_found")
data class MatchFound(
    val roomId: String,
    val opponent: Player,
    val playerColor: String
) : WsServerMessage

@Serializable
@SerialName("chat_receive")
data class ChatReceive(
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isSpectator: Boolean = false
) : WsServerMessage

@Serializable
@SerialName("timer_update")
data class TimerUpdate(
    val roomId: String,
    val redTimeMillis: Long,
    val blackTimeMillis: Long
) : WsServerMessage

@Serializable
@SerialName("opponent_joined")
data class OpponentJoined(
    val roomId: String,
    val opponent: Player
) : WsServerMessage

@Serializable
@SerialName("opponent_disconnected")
data class OpponentDisconnected(
    val roomId: String
) : WsServerMessage


@Serializable
@SerialName("opponent_reconnected")
data class OpponentReconnected(
    val roomId: String
) : WsServerMessage

@Serializable
@SerialName("draw_offered")
data class DrawOffered(
    val roomId: String,
    val offeredBy: String
) : WsServerMessage

@Serializable
@SerialName("undo_requested")
data class UndoRequested(
    val roomId: String,
    val requestedBy: String
) : WsServerMessage

@Serializable
@SerialName("spectator_joined")
data class SpectatorJoined(
    val roomId: String,
    val spectator: Player
) : WsServerMessage

@Serializable
@SerialName("spectator_left")
data class SpectatorLeft(
    val roomId: String,
    val spectatorId: String
) : WsServerMessage

@Serializable
@SerialName("xp_update")
data class XpUpdate(
    val newXp: Int,
    val newLevel: Int,
    val xpGained: Int
) : WsServerMessage

// ---- Server-to-client messages (global scene) ----

@Serializable
@SerialName("global_chat_receive")
data class GlobalChatReceive(
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long
) : WsServerMessage

@Serializable
@SerialName("error")
data class Error(
    val code: String,
    val message: String
) : WsServerMessage
