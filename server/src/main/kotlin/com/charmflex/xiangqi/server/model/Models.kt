package com.charmflex.xiangqi.server.model

import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.model.GameStatus
import jakarta.persistence.*
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


private const val MAX_LEVEL = 52


@Serializable
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val xp: Int = 0,
    val level: Int = 1
) {
    companion object {
        fun computeLevel(xp: Int): Int {
            var level = 1
            var remaining = xp

            while (level < MAX_LEVEL) {
                val xpRequired = xpToNextLevel(level)
                if (remaining < xpRequired) break
                remaining -= xpRequired
                level++
            }

            return level
        }


        fun xpToNextLevel(level: Int): Int {
            return (100 * Math.pow(1.025, (level - 1).toDouble())).toInt()
        }
    }
}

fun Player.isBot(): Boolean = id.startsWith("bot-")

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

data class ChatEntry(
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isSpectator: Boolean = false
)

data class GameRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var redPlayer: Player? = null,
    var blackPlayer: Player? = null,
    var status: RoomStatus = RoomStatus.WAITING,
    val timeControlSeconds: Int = 600,
    val private: Boolean = false,
    val password: String? = null,
    // CopyOnWriteArrayList: safe for concurrent iteration during broadcasts
    val moves: MutableList<MoveDto> = java.util.concurrent.CopyOnWriteArrayList(),
    var currentTurn: String = "RED",
    var redTimeMillis: Long = 600_000L,
    var blackTimeMillis: Long = 600_000L,
    var lastMoveTimestamp: Long = 0L,
    // ConcurrentHashMap: safe for concurrent spectator join/leave + chat reads
    val spectators: MutableMap<String, Player> = ConcurrentHashMap(),
    // Tracks whether the game clock has been started (prevents timestamp reset on reconnect)
    var gameStarted: Boolean = false,
    // In-memory chat history retained for reconnect/spectator snapshots
    val chatHistory: MutableList<ChatEntry> = java.util.concurrent.CopyOnWriteArrayList()
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
    val isPrivate: Boolean = false,
    val password: String? = null
)

data class JoinRoomRequest(val password: String? = null)

data class CreateRoomResponse(val roomId: String)

data class BattleRoomResponse(
    val id: String,
    val name: String,
    val host: Player? = null,
    val guest: Player? = null,
    val status: String,
    val timeControlSeconds: Int,
    val isPrivate: Boolean,
    val hasPassword: Boolean = false
)

data class ActiveRoomsResponse(val rooms: List<BattleRoomResponse>)

data class ActiveGameResponse(
    val roomId: String,
    val opponentName: String,
    val playerColor: String,
    val redTimeMillis: Long,
    val blackTimeMillis: Long
)

data class AdminChatRequest(val message: String)

data class LeaderboardEntry(
    val name: String,
    val xp: Int,
    val level: Int
)

data class LeaderboardResponse(
    val entries: List<LeaderboardEntry>
)
