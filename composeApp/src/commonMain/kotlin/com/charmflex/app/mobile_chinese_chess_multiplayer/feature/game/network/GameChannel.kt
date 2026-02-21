package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.network

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.RealTimeCommunicationChannel
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WsServerMessage
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.model.MoveDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.annotation.Singleton

@Singleton
class GameChannel(
    private val socketClient: WebSocketClient
) : RealTimeCommunicationChannel(socketClient) {
    private val wsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun subscribeChannel(): Flow<WsServerMessage> {
        return channelMessage
            .mapNotNull {
                when (it.type) {
                    "move_made" -> wsJson.decodeFromJsonElement<MoveMade>(it.payload)
                    "game_started" -> wsJson.decodeFromJsonElement<GameStarted>(it.payload)
                    "game_state" -> wsJson.decodeFromJsonElement<RoomSnapshot>(it.payload)
                    "game_over" -> wsJson.decodeFromJsonElement<GameOver>(it.payload)
                    "queue_update" -> wsJson.decodeFromJsonElement<QueueUpdate>(it.payload)
                    "match_found" -> wsJson.decodeFromJsonElement<MatchFound>(it.payload)
                    "chat_receive" -> wsJson.decodeFromJsonElement<ChatReceive>(it.payload)
                    "timer_update" -> wsJson.decodeFromJsonElement<TimerUpdate>(it.payload)
                    "opponent_joined" -> wsJson.decodeFromJsonElement<OpponentJoined>(it.payload)
                    "opponent_disconnected" -> wsJson.decodeFromJsonElement<OpponentDisconnected>(it.payload)
                    "opponent_reconnected" -> wsJson.decodeFromJsonElement<OpponentReconnected>(it.payload)
                    "draw_offered" -> wsJson.decodeFromJsonElement<DrawOffered>(it.payload)
                    "undo_requested" -> wsJson.decodeFromJsonElement<UndoRequested>(it.payload)
                    "spectator_joined" -> wsJson.decodeFromJsonElement<SpectatorJoined>(it.payload)
                    "spectator_left" -> wsJson.decodeFromJsonElement<SpectatorLeft>(it.payload)
                    "xp_update" -> wsJson.decodeFromJsonElement<XpUpdate>(it.payload)
                    "error" -> wsJson.decodeFromJsonElement<Error>(it.payload)
                    else -> null
                }
            }
    }

    suspend fun sendMove(roomId: String, move: MoveDto) {
        socketClient.send(MakeMove(roomId, move))
    }

    suspend fun joinQueue(timeControlSeconds: Int = 600) {
        println("[WS] Joining matchmaking queue (time=${timeControlSeconds}s)")
        socketClient.send(QueueJoin(timeControlSeconds))
    }

    suspend fun leaveQueue() {
        println("[WS] Leaving matchmaking queue")
        socketClient.send(QueueLeave)
    }

    suspend fun sendChat(roomId: String, message: String) {
        socketClient.send(ChatSend(roomId, message))
    }

    suspend fun resign(roomId: String) {
        println("[WS] Resign in room $roomId")
        socketClient.send(Resign(roomId))
    }

    suspend fun offerDraw(roomId: String) {
        socketClient.send(DrawOffer(roomId))
    }

    suspend fun respondToDraw(roomId: String, accepted: Boolean) {
        socketClient.send(DrawResponse(roomId, accepted))
    }

    suspend fun joinRoom(roomId: String) {
        println("[WS] Joining room WS: $roomId")
        socketClient.send(RoomJoin(roomId))
    }

    suspend fun watchRoom(roomId: String) {
        println("[WS] Joining room as spectator: $roomId")
        socketClient.send(RoomJoin(roomId))
    }

    suspend fun abandonGame(roomId: String) {
        println("[WS] Joining room as spectator: $roomId")
        socketClient.send(RoomAbandon(roomId))
    }

    suspend fun reportGameOver(roomId: String, result: String, reason: String) {
        println("[WS] Reporting game over: room=$roomId result=$result reason=$reason")
        socketClient.send(GameClientOverReport(roomId, result, reason))
    }

    override fun scene(): RealTimeCommunicationChannel.ChannelScene {
        return GameChannelScene
    }
}

private val GameChannelScene = object : RealTimeCommunicationChannel.ChannelScene {
    override val name: String get() = "game"
}
