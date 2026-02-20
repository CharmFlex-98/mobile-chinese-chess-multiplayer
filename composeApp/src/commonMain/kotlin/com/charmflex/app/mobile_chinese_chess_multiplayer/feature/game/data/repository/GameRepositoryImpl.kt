package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.data.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.NetworkAttributes
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.NetworkClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WsServerMessage
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.usePost
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.utils.resultOf
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.model.MoveDto
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.ActiveRoomsResponse
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.BattleRoom
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.CreateRoomRequest
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.CreateRoomResponse
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.GameRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.network.GameChannel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.network.GlobalChatChannel
import com.charmflex.xiangqi.engine.model.Move
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.koin.core.annotation.Singleton

@Singleton
class GameRepositoryImpl(
    private val networkClient: NetworkClient,
    private val gameChannel: GameChannel,
    private val globalChatChannel: GlobalChatChannel
) : GameRepository {
    override suspend fun connectLobby() {
        gameChannel.connectWebSocket()
    }

    override fun isConnected(): Flow<Boolean> {
        return gameChannel.isConnected()
    }

    override fun subscribeMatchRoomEvents(roomId: String): Flow<WsServerMessage> {
        return gameChannel.subscribeChannel()
            .filter {
                when (it) {
                    is MoveMade -> it.roomId == roomId
                    is GameOver -> it.roomId == roomId
                    is GameStarted -> it.roomId == roomId
                    is RoomSnapshot -> it.roomId == roomId
                    is TimerUpdate -> it.roomId == roomId
                    is ChatReceive -> it.roomId == roomId
                    is OpponentJoined -> it.roomId == roomId
                    is OpponentDisconnected -> it.roomId == roomId
                    is OpponentReconnected -> it.roomId == roomId
                    is DrawOffered -> it.roomId == roomId
                    is UndoRequested -> it.roomId == roomId
                    is SpectatorJoined -> it.roomId == roomId
                    is SpectatorLeft -> it.roomId == roomId
                    is XpUpdate -> true
                    else -> false
                }
            }
    }

    override fun subscribeMatchingEvents(): Flow<WsServerMessage> {
        return gameChannel.subscribeChannel()
            .filter { it is QueueUpdate || it is MatchFound }
    }

    override fun subscribeGlobalChat(): Flow<WsServerMessage> {
        return globalChatChannel.subscribeChannel()
    }

    override suspend fun createRoom(createRoomRequest: CreateRoomRequest): Result<CreateRoomResponse> {
        return resultOf {
            val response: CreateRoomResponse =
                networkClient.usePost("/api/rooms/create", createRoomRequest) {
                    add(NetworkAttributes.needToken)
                }
            response
        }
    }

    override suspend fun getActiveRooms(): Result<List<BattleRoom>> {
        return resultOf {
            val response: ActiveRoomsResponse = networkClient.useGet("/api/rooms") {
                add(NetworkAttributes.needToken)
            }
            response.rooms
        }
    }

    override suspend fun joinRoom(roomId: String): Result<BattleRoom> {
        return resultOf {
            val response: BattleRoom = networkClient.usePost("/api/rooms/${roomId}/join", Unit) {
                add(NetworkAttributes.needToken)
            }
            response
        }
    }

    override suspend fun joinMatchmaking(timeControlSeconds: Int) {
        gameChannel.joinQueue(timeControlSeconds)
    }

    override suspend fun leaveMatchmaking() {
        gameChannel.leaveQueue()
    }

    override suspend fun sendMove(roomId: String, move: Move) {
        val moveDto = MoveDto(
            fromRow = move.from.row,
            fromCol = move.from.col,
            toRow = move.to.row,
            toCol = move.to.col
        )
        gameChannel.sendMove(roomId, moveDto)
    }

    override suspend fun sendChat(roomId: String, message: String) {
        gameChannel.sendChat(roomId, message)
    }

    override suspend fun sendGlobalChat(message: String) {
        globalChatChannel.sendGlobalChat(message)
    }

    override suspend fun resign(roomId: String) {
        gameChannel.resign(roomId)
    }

    override suspend fun offerDraw(roomId: String) {
        gameChannel.offerDraw(roomId)
    }

    override suspend fun respondToDraw(roomId: String, accepted: Boolean) {
        gameChannel.respondToDraw(roomId, accepted)
    }

    override suspend fun joinRoomWs(roomId: String) {
        gameChannel.joinRoom(roomId)
    }

    override suspend fun watchRoom(roomId: String) {
        gameChannel.watchRoom(roomId)
    }

    override suspend fun abandonGame(roomId: String) {
        gameChannel.abandonGame(roomId)
    }

    override suspend fun reportGameOver(roomId: String, result: String, reason: String) {
        gameChannel.reportGameOver(roomId, result, reason)
    }
}
