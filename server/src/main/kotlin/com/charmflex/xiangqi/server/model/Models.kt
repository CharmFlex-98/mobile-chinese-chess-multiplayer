package com.charmflex.xiangqi.server.model

import com.charmflex.xiangqi.engine.ai.AiDifficulty
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val xp: Int = 0,
    val level: Int = 1
) {
    companion object {
        fun computeLevel(xp: Int): Int = 1 + xp / 200
    }
}

@Serializable
data class MoveDto(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int
)

data class GameRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var redPlayer: Player? = null,
    var blackPlayer: Player? = null,
    var status: RoomStatus = RoomStatus.WAITING,
    val timeControlSeconds: Int = 600,
    val private: Boolean = false,
    val moves: MutableList<MoveDto> = mutableListOf(),
    var currentTurn: String = "RED",
    var redTimeMillis: Long = 600_000L,
    var blackTimeMillis: Long = 600_000L,
    var lastMoveTimestamp: Long = System.currentTimeMillis(),
    val spectators: MutableMap<String, Player> = mutableMapOf()
)

enum class RoomStatus {
    WAITING, PLAYING, FINISHED
}

data class QueueEntry(
    val player: Player,
    val sessionId: String,
    val timeControlSeconds: Int,
    val joinedAt: Long = System.currentTimeMillis()
)

data class BotPlayer(
    val player: Player,
    val difficulty: AiDifficulty,
    val minDelayMs: Long,
    val maxDelayMs: Long
)

@Serializable
data class AuthResponse(
    val token: String,
    val player: Player
)

// REST request/response models

data class GuestLoginRequest(val username: String = "", val displayName: String = "")

data class CreateRoomRequest(
    val name: String,
    val timeControlSeconds: Int = 600,
    val isPrivate: Boolean = false
)

data class CreateRoomResponse(val roomId: String)

data class BattleRoomResponse(
    val id: String,
    val name: String,
    val host: Player,
    val guest: Player? = null,
    val status: String,
    val timeControlSeconds: Int,
    val isPrivate: Boolean
)

data class ActiveRoomsResponse(val rooms: List<BattleRoomResponse>)
