package com.charmflex.xiangqi.server.model

import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.model.GameStatus
import jakarta.persistence.*
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

@Entity
@Table(name = "players")
data class PlayerEntity(
    @Id
    val id: String,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val xp: Int = 0,
    @Column(nullable = false)
    val level: Int = 1
) {
    fun toPlayer(): Player = Player(id = id, name = name, xp = xp, level = level)
}

@Serializable
data class MoveDto(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int
)

/**
 * Result returned from GameService.makeMove(). Carries everything the handler needs
 * so it never has to reach back into a mutable GameRoom after the call returns.
 */
data class MakeMoveResult(
    val success: Boolean,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val timedOut: Boolean = false,
    val redTimeMillis: Long = 0L,
    val blackTimeMillis: Long = 0L
)

data class GameRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var redPlayer: Player? = null,
    var blackPlayer: Player? = null,
    var status: RoomStatus = RoomStatus.WAITING,
    val timeControlSeconds: Int = 600,
    val private: Boolean = false,
    // CopyOnWriteArrayList: safe for concurrent iteration during broadcasts
    val moves: MutableList<MoveDto> = java.util.concurrent.CopyOnWriteArrayList(),
    var currentTurn: String = "RED",
    var redTimeMillis: Long = 600_000L,
    var blackTimeMillis: Long = 600_000L,
    var lastMoveTimestamp: Long = 0L,
    // ConcurrentHashMap: safe for concurrent spectator join/leave + chat reads
    val spectators: MutableMap<String, Player> = ConcurrentHashMap(),
    // Tracks whether the game clock has been started (prevents timestamp reset on reconnect)
    var gameStarted: Boolean = false
) {
    val isEmpty: Boolean
        get() = redPlayer == null && blackPlayer == null
}

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
    val host: Player? = null,
    val guest: Player? = null,
    val status: String,
    val timeControlSeconds: Int,
    val isPrivate: Boolean
)

data class ActiveRoomsResponse(val rooms: List<BattleRoomResponse>)
