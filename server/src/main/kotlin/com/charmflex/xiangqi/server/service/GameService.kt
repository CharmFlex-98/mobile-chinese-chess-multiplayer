package com.charmflex.xiangqi.server.service

import com.charmflex.xiangqi.server.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class GameService {
    private val log = LoggerFactory.getLogger(GameService::class.java)
    private val rooms = ConcurrentHashMap<String, GameRoom>()
    private val players = ConcurrentHashMap<String, Player>()
    private val matchmakingQueue = ConcurrentHashMap<String, QueueEntry>()
    // sessionId -> playerId
    private val sessionPlayerMap = ConcurrentHashMap<String, String>()

    fun createGuestPlayer(name: String): Player {
        val player = Player(name = name)
        players[player.id] = player
        log.info("[SVC] Created guest player: id={} name={} (total players={})", player.id.take(8), player.name, players.size)
        return player
    }

    fun getOrCreatePlayer(id: String, name: String): Player {
        val existing = players[id]
        if (existing != null) {
            log.info("[SVC] getOrCreatePlayer: found existing id={} name={}", id.take(8), existing.name)
            return existing
        }
        val player = Player(id = id, name = name)
        players[player.id] = player
        log.info("[SVC] getOrCreatePlayer: created new id={} name={} (total={})", id.take(8), name, players.size)
        return player
    }

    fun getPlayerById(playerId: String): Player? {
        val p = players[playerId]
        if (p == null) log.warn("[SVC] getPlayerById: {} NOT FOUND (known players={})", playerId.take(8), players.size)
        return p
    }

    fun registerPlayer(sessionId: String, name: String): Player {
        val existing = sessionPlayerMap[sessionId]?.let { players[it] }
        if (existing != null) {
            log.info("[SVC] registerPlayer: session {} already linked to player {}", sessionId, existing.name)
            return existing
        }

        val player = Player(name = name)
        players[player.id] = player
        sessionPlayerMap[sessionId] = player.id
        log.info("[SVC] registerPlayer: session {} -> player id={} name={}", sessionId, player.id.take(8), player.name)
        return player
    }

    fun registerPlayerByToken(sessionId: String, token: String): Player? {
        val player = players[token] ?: run {
            log.warn("[SVC] registerPlayerByToken: token {} not found (known={})", token.take(8), players.keys.map { it.take(8) })
            return null
        }
        sessionPlayerMap[sessionId] = player.id
        log.info("[SVC] registerPlayerByToken: session {} -> player id={} name={}", sessionId, player.id.take(8), player.name)
        return player
    }

    fun getPlayerBySession(sessionId: String): Player? {
        val playerId = sessionPlayerMap[sessionId] ?: return null
        return players[playerId]
    }

    fun getSessionIdForPlayer(playerId: String): String? {
        return sessionPlayerMap.entries.firstOrNull { it.value == playerId }?.key
    }

    fun createRoom(player: Player, name: String, timeControlSeconds: Int, isPrivate: Boolean): GameRoom {
        val room = GameRoom(
            name = name,
            redPlayer = player,
            timeControlSeconds = timeControlSeconds,
            private = isPrivate,
            redTimeMillis = timeControlSeconds * 1000L,
            blackTimeMillis = timeControlSeconds * 1000L
        )
        rooms[room.id] = room
        log.info("[SVC] Room created: id={} name={} host={} (total rooms={})", room.id, name, player.name, rooms.size)
        return room
    }

    fun joinRoom(roomId: String, player: Player): GameRoom? {
        val room = rooms[roomId] ?: run {
            log.warn("[SVC] joinRoom: room {} not found", roomId)
            return null
        }
        if (room.status != RoomStatus.WAITING) {
            log.warn("[SVC] joinRoom: room {} status={} (not WAITING)", roomId, room.status)
            return null
        }
        if (room.redPlayer?.id == player.id) {
            log.info("[SVC] joinRoom: player {} is already red in room {}", player.name, roomId)
            return room
        }

        room.blackPlayer = player
        room.status = RoomStatus.PLAYING
        room.lastMoveTimestamp = System.currentTimeMillis()
        log.info("[SVC] joinRoom OK: room={} red={} black={} status=PLAYING", roomId, room.redPlayer?.name, player.name)
        return room
    }

    fun getActiveRooms(): List<GameRoom> {
        val active = rooms.values.filter { !it.private && it.status != RoomStatus.FINISHED }
        log.info("[SVC] getActiveRooms: {} active out of {} total", active.size, rooms.size)
        return active
    }

    fun getRoom(roomId: String): GameRoom? = rooms[roomId]

    fun makeMove(roomId: String, move: MoveDto, playerId: String): Boolean {
        val room = rooms[roomId] ?: run {
            log.warn("[SVC] makeMove: room {} not found", roomId)
            return false
        }
        if (room.status != RoomStatus.PLAYING) {
            log.warn("[SVC] makeMove: room {} status={}", roomId, room.status)
            return false
        }

        val isRedTurn = room.currentTurn == "RED"
        val currentPlayer = if (isRedTurn) room.redPlayer else room.blackPlayer
        if (currentPlayer?.id != playerId) {
            log.warn("[SVC] makeMove: not {}'s turn (current={}, expected={})", playerId.take(8), room.currentTurn, currentPlayer?.name)
            return false
        }
        log.info("[SVC] makeMove: room={} player={} turn={} ({},{})->({},{})", roomId, currentPlayer.name, room.currentTurn, move.fromRow, move.fromCol, move.toRow, move.toCol)

        // Update timer
        val now = System.currentTimeMillis()
        val elapsed = now - room.lastMoveTimestamp
        if (isRedTurn) {
            room.redTimeMillis -= elapsed
        } else {
            room.blackTimeMillis -= elapsed
        }
        room.lastMoveTimestamp = now

        // Check timeout
        if (room.redTimeMillis <= 0 || room.blackTimeMillis <= 0) {
            room.status = RoomStatus.FINISHED
            return true
        }

        room.moves.add(move)
        room.currentTurn = if (isRedTurn) "BLACK" else "RED"
        return true
    }

    fun resign(roomId: String, playerId: String): String? {
        val room = rooms[roomId] ?: return null
        room.status = RoomStatus.FINISHED
        return if (room.redPlayer?.id == playerId) "black_wins" else "red_wins"
    }

    fun addXp(playerId: String, xpGain: Int): Player {
        val player = players[playerId] ?: return Player(id = playerId, name = "Unknown")
        val newXp = player.xp + xpGain
        val newLevel = Player.computeLevel(newXp)
        val updated = player.copy(xp = newXp, level = newLevel)
        players[playerId] = updated
        log.info("[SVC] XP added: player={} xp={}+{}={} level={}", player.name, player.xp, xpGain, newXp, newLevel)
        return updated
    }

    // Matchmaking
    fun joinQueue(sessionId: String, player: Player, timeControlSeconds: Int) {
        matchmakingQueue[sessionId] = QueueEntry(player, sessionId, timeControlSeconds)
    }

    fun leaveQueue(sessionId: String) {
        matchmakingQueue.remove(sessionId)
    }

    fun findMatch(sessionId: String): Pair<QueueEntry, QueueEntry>? {
        val entry = matchmakingQueue[sessionId] ?: return null
        val opponent = matchmakingQueue.values.firstOrNull {
            it.sessionId != sessionId && it.timeControlSeconds == entry.timeControlSeconds
        } ?: return null

        matchmakingQueue.remove(sessionId)
        matchmakingQueue.remove(opponent.sessionId)
        return entry to opponent
    }

    fun getQueuePosition(sessionId: String): Int {
        val entries = matchmakingQueue.values.sortedBy { it.joinedAt }
        return entries.indexOfFirst { it.sessionId == sessionId } + 1
    }

    fun removeSession(sessionId: String) {
        val playerId = sessionPlayerMap[sessionId]
        log.info("[SVC] removeSession: session={} player={}", sessionId, playerId?.take(8))
        matchmakingQueue.remove(sessionId)
        sessionPlayerMap.remove(sessionId)
    }

    // --- Bot helpers ---

    fun registerBotPlayer(player: Player) {
        players[player.id] = player
        log.info("[SVC] Registered bot player: id={} name={}", player.id.take(8), player.name)
    }

    fun registerBotSession(sessionId: String, player: Player) {
        sessionPlayerMap[sessionId] = player.id
        log.info("[SVC] Registered bot session: session={} player={}", sessionId, player.name)
    }

    fun isBotSession(sessionId: String): Boolean = sessionId.startsWith("bot-")

    fun getQueueEntry(sessionId: String): QueueEntry? = matchmakingQueue[sessionId]

    fun roomHasBot(roomId: String): Boolean {
        val room = rooms[roomId] ?: return false
        return (room.redPlayer?.id?.startsWith("bot-") == true) ||
               (room.blackPlayer?.id?.startsWith("bot-") == true)
    }
}
