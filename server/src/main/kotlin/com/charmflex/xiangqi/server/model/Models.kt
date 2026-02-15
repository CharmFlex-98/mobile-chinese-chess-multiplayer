package com.charmflex.xiangqi.server.model

import com.charmflex.xiangqi.engine.ai.AiDifficulty
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rating: Int = 1200
)

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
    val isPrivate: Boolean = false,
    val moves: MutableList<MoveDto> = mutableListOf(),
    var currentTurn: String = "RED",
    var redTimeMillis: Long = 600_000L,
    var blackTimeMillis: Long = 600_000L,
    var lastMoveTimestamp: Long = System.currentTimeMillis()
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
