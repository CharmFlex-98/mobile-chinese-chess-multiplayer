package com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class WsEnvelope(
    val type: String,
    val payload: JsonElement
)

@Serializable
sealed interface WsClientMessage {
    val type: String

    @Serializable
    @SerialName("make_move")
    data class MakeMove(
        val roomId: String,
        val move: MoveDto
    ) : WsClientMessage {
        override val type: String get() = "make_move"
    }

    @Serializable
    @SerialName("queue_join")
    data class QueueJoin(
        val timeControlSeconds: Int = 600
    ) : WsClientMessage {
        override val type: String get() = "queue_join"
    }

    @Serializable
    @SerialName("queue_leave")
    data object QueueLeave : WsClientMessage {
        override val type: String get() = "queue_leave"
    }

    @Serializable
    @SerialName("chat_send")
    data class ChatSend(
        val roomId: String,
        val message: String
    ) : WsClientMessage {
        override val type: String get() = "chat_send"
    }

    @Serializable
    @SerialName("resign")
    data class Resign(
        val roomId: String
    ) : WsClientMessage {
        override val type: String get() = "resign"
    }

    @Serializable
    @SerialName("draw_offer")
    data class DrawOffer(
        val roomId: String
    ) : WsClientMessage {
        override val type: String get() = "draw_offer"
    }

    @Serializable
    @SerialName("draw_response")
    data class DrawResponse(
        val roomId: String,
        val accepted: Boolean
    ) : WsClientMessage {
        override val type: String get() = "draw_response"
    }

    @Serializable
    @SerialName("undo_request")
    data class UndoRequest(
        val roomId: String
    ) : WsClientMessage {
        override val type: String get() = "undo_request"
    }

    @Serializable
    @SerialName("undo_response")
    data class UndoResponse(
        val roomId: String,
        val accepted: Boolean
    ) : WsClientMessage {
        override val type: String get() = "undo_response"
    }

    @Serializable
    @SerialName("room_join")
    data class RoomJoin(
        val roomId: String
    ) : WsClientMessage {
        override val type: String get() = "room_join"
    }

    @Serializable
    @SerialName("game_over_report")
    data class GameOverReport(
        val roomId: String,
        val result: String,
        val reason: String
    ) : WsClientMessage {
        override val type: String get() = "game_over_report"
    }
}

@Serializable
sealed interface WsServerMessage {
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
        val redPlayer: PlayerDto,
        val blackPlayer: PlayerDto,
        val timeControlSeconds: Int
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
        val opponent: PlayerDto,
        val playerColor: String
    ) : WsServerMessage

    @Serializable
    @SerialName("chat_receive")
    data class ChatReceive(
        val roomId: String,
        val senderId: String,
        val senderName: String,
        val message: String,
        val timestamp: Long
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
        val opponent: PlayerDto
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
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : WsServerMessage
}

val wsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}
