package com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.ApiClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.*
import com.charmflex.xiangqi.engine.model.Move
import com.charmflex.xiangqi.engine.model.PieceColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class GameRepository(
    private val apiClient: ApiClient,
    private val webSocketClient: WebSocketClient
) {
    val serverMessages: SharedFlow<WsServerMessage> = webSocketClient.serverMessages
    val isConnected: StateFlow<Boolean> = webSocketClient.connectionState

    suspend fun connectWebSocket(scope: CoroutineScope) {
        webSocketClient.connect(scope)
    }

    fun disconnectWebSocket() {
        webSocketClient.disconnect()
    }

    suspend fun getActiveRooms(): Result<List<RoomDto>> {
        return try {
            Result.success(apiClient.getActiveRooms())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRoom(
        name: String,
        timeControlSeconds: Int = 600,
        isPrivate: Boolean = false
    ): Result<String> {
        return try {
            val response = apiClient.createRoom(
                CreateRoomRequest(name, timeControlSeconds, isPrivate)
            )
            Result.success(response.roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomId: String): Result<RoomDto> {
        return try {
            Result.success(apiClient.joinRoom(roomId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinMatchmaking(timeControlSeconds: Int = 600) {
        webSocketClient.joinQueue(timeControlSeconds)
    }

    suspend fun leaveMatchmaking() {
        webSocketClient.leaveQueue()
    }

    suspend fun sendMove(roomId: String, move: Move) {
        val moveDto = MoveDto(
            fromRow = move.from.row,
            fromCol = move.from.col,
            toRow = move.to.row,
            toCol = move.to.col
        )
        webSocketClient.sendMove(roomId, moveDto)
    }

    suspend fun sendChat(roomId: String, message: String) {
        webSocketClient.sendChat(roomId, message)
    }

    suspend fun resign(roomId: String) {
        webSocketClient.resign(roomId)
    }

    suspend fun offerDraw(roomId: String) {
        webSocketClient.offerDraw(roomId)
    }

    suspend fun respondToDraw(roomId: String, accepted: Boolean) {
        webSocketClient.respondToDraw(roomId, accepted)
    }

    suspend fun joinRoomWs(roomId: String) {
        webSocketClient.joinRoom(roomId)
    }

    suspend fun reportGameOver(roomId: String, result: String, reason: String) {
        webSocketClient.reportGameOver(roomId, result, reason)
    }

    fun observeGameEvents(roomId: String): Flow<WsServerMessage> {
        return serverMessages.filter { msg ->
            when (msg) {
                is WsServerMessage.MoveMade -> msg.roomId == roomId
                is WsServerMessage.GameOver -> msg.roomId == roomId
                is WsServerMessage.GameStarted -> msg.roomId == roomId
                is WsServerMessage.TimerUpdate -> msg.roomId == roomId
                is WsServerMessage.ChatReceive -> msg.roomId == roomId
                is WsServerMessage.OpponentJoined -> msg.roomId == roomId
                is WsServerMessage.OpponentDisconnected -> msg.roomId == roomId
                is WsServerMessage.OpponentReconnected -> msg.roomId == roomId
                is WsServerMessage.DrawOffered -> msg.roomId == roomId
                is WsServerMessage.UndoRequested -> msg.roomId == roomId
                else -> false
            }
        }
    }

    fun observeMatchmaking(): Flow<WsServerMessage> {
        return serverMessages.filter { msg ->
            msg is WsServerMessage.QueueUpdate || msg is WsServerMessage.MatchFound
        }
    }
}
