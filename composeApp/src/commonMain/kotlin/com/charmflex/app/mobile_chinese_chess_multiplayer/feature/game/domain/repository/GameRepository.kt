package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WsServerMessage
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.profile.Player
import com.charmflex.xiangqi.engine.model.Move
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface GameRepository {
    suspend fun connectLobby()
    fun isConnected(): Flow<Boolean>
    fun subscribeMatchRoomEvents(roomId: String): Flow<WsServerMessage>
    fun subscribeMatchingEvents(): Flow<WsServerMessage>
    fun subscribeGlobalChat(): Flow<WsServerMessage>
    fun subscribeRejoinEvents(): Flow<WsServerMessage>
    suspend fun createRoom(createRoomRequest: CreateRoomRequest): Result<CreateRoomResponse>
    suspend fun getActiveRooms(): Result<List<BattleRoom>>
    suspend fun joinRoom(roomId: String): Result<BattleRoom>
    suspend fun joinMatchmaking(timeControlSeconds: Int = 600)
    suspend fun leaveMatchmaking()
    suspend fun sendMove(roomId: String, move: Move)
    suspend fun sendChat(roomId: String, message: String)
    suspend fun sendGlobalChat(message: String)
    suspend fun resign(roomId: String)
    suspend fun offerDraw(roomId: String)
    suspend fun respondToDraw(roomId: String, accepted: Boolean)
    suspend fun joinRoomWs(roomId: String)
    suspend fun watchRoom(roomId: String)
    suspend fun abandonGame(roomId: String)
    suspend fun reportGameOver(roomId: String, result: String, reason: String)
}

@Serializable
data class CreateRoomRequest(
    val name: String,
    val timeControlSeconds: Int = 600,
    val isPrivate: Boolean = false
)

@Serializable
data class CreateRoomResponse(
    val roomId: String
)

@Serializable
data class BattleRoom(
    val id: String,
    val name: String,
    val host: Player?,
    val guest: Player? = null,
    val status: String = "waiting",
    val timeControlSeconds: Int = 600,
    val private: Boolean = false
) {
    val isPlayingRoom: Boolean = guest != null && host != null
    val playingCount: Int
        get() = if (guest != null && host != null) {
            2
        } else if (guest == null && host == null) {
            0
        } else {
            1
        }
}

@Serializable
data class ActiveRoomsResponse(
    val rooms: List<BattleRoom>
)
