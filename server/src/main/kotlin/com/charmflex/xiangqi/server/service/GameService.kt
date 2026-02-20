package com.charmflex.xiangqi.server.service

import com.charmflex.xiangqi.engine.model.*
import com.charmflex.xiangqi.engine.rules.GameRules
import com.charmflex.xiangqi.server.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class GameService {
    private val log = LoggerFactory.getLogger(GameService::class.java)
    private val rooms = ConcurrentHashMap<String, GameRoom>()
    private val matchmakingQueue = ConcurrentHashMap<String, QueueEntry>()

    // roomId -> live board state; mirrors the move history for server-side game-status checks
    private val roomBoards = ConcurrentHashMap<String, Board>()
    // roomId -> set of sessionIds (players + spectators)
    private val roomSessionIdsMap = ConcurrentHashMap<String, MutableSet<String>>()

    // -------------------------------------------------------------------------
    // Player management
    // -------------------------------------------------------------------------

    fun createGuestPlayer(name: String): Player {
        val player = Player(name = name)
        log.info("[SVC] Created guest player: id={} name={}", player.id.take(8), player.name)
        return player
    }

    // -------------------------------------------------------------------------
    // Room management
    // -------------------------------------------------------------------------

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
        roomBoards[room.id] = Board.initial()
        log.info("[SVC] Room created: id={} name={} host={} (total rooms={})", room.id, name, player.name, rooms.size)
        return room
    }

    fun joinRoom(roomId: String, player: Player): GameRoom? {
        val room = rooms[roomId] ?: run {
            log.warn("[SVC] joinRoom: room {} not found", roomId)
            return null
        }
        synchronized(room) {
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
            log.info("[SVC] joinRoom OK: room={} red={} black={} status=PLAYING", roomId, room.redPlayer?.name, player.name)
            return room
        }
    }

    /**
     * Records the game clock start time for a room. Called once when both players' WebSocket
     * sessions have joined the room. Guards against resetting the timestamp on reconnect.
     * Returns true if the game was freshly started, false if it was already started.
     */
    fun recordGameStart(roomId: String): Boolean {
        val room = rooms[roomId] ?: return false
        synchronized(room) {
            if (room.gameStarted) return false
            room.gameStarted = true
            room.lastMoveTimestamp = System.currentTimeMillis()
            return true
        }
    }

    fun getActiveRooms(): List<GameRoom> {
        val active = rooms.values.filter { !it.private && it.status != RoomStatus.FINISHED }
        log.info("[SVC] getActiveRooms: {} active out of {} total", active.size, rooms.size)
        return active
    }

    fun getRoom(roomId: String): GameRoom? = rooms[roomId]

    fun findActiveRoomForPlayer(playerId: String): GameRoom? =
        rooms.values.firstOrNull {
            it.status == RoomStatus.PLAYING &&
            (it.redPlayer?.id == playerId || it.blackPlayer?.id == playerId)
        }

    fun removeRoom(roomId: String): GameRoom? {
        roomBoards.remove(roomId)
        roomSessionIdsMap.remove(roomId)
        return rooms.remove(roomId)
    }

    /**
     * Atomically marks a room as FINISHED. Returns false if the room was already finished
     * or does not exist, preventing double-processing (e.g. double XP grants).
     */
    fun finishGame(roomId: String): Boolean {
        val room = rooms[roomId] ?: return false
        synchronized(room) {
            if (room.status == RoomStatus.FINISHED) return false
            room.status = RoomStatus.FINISHED
            return true
        }
    }

    /** Convenience alias used for draw agreements. */
    fun markAsDraw(roomId: String): Boolean = finishGame(roomId)

    /**
     * Applies a move server-side, updates timers, and evaluates game status via the engine.
     * All compound mutations are performed under a per-room lock to prevent races.
     *
     * Returns a [MakeMoveResult] the caller uses to determine next action (broadcast,
     * game-over handling, etc.) without needing to touch the mutable GameRoom directly.
     */
    fun makeMove(roomId: String, move: MoveDto, playerId: String): MakeMoveResult {
        val room = rooms[roomId] ?: run {
            log.warn("[SVC] makeMove: room {} not found", roomId)
            return MakeMoveResult(success = false)
        }
        if (room.status != RoomStatus.PLAYING) {
            log.warn("[SVC] makeMove: room {} status={}", roomId, room.status)
            return MakeMoveResult(success = false)
        }

        synchronized(room) {
            if (room.status != RoomStatus.PLAYING) {
                log.warn("[SVC] makeMove: room {} status={} (re-check inside lock)", roomId, room.status)
                return MakeMoveResult(success = false)
            }
            val isRedTurn = room.currentTurn == "RED"
            val currentPlayer = if (isRedTurn) room.redPlayer else room.blackPlayer
            if (currentPlayer?.id != playerId) {
                log.warn("[SVC] makeMove: not {}'s turn (current={}, expected={})", playerId.take(8), room.currentTurn, currentPlayer?.name)
                return MakeMoveResult(success = false)
            }
            log.info("[SVC] makeMove: room={} player={} turn={} ({},{})->({},{})", roomId, currentPlayer.name, room.currentTurn, move.fromRow, move.fromCol, move.toRow, move.toCol)

            // Update timer
            val now = System.currentTimeMillis()
            val elapsed = now - room.lastMoveTimestamp
            if (isRedTurn) room.redTimeMillis -= elapsed
            else room.blackTimeMillis -= elapsed
            room.lastMoveTimestamp = now

            // Check timeout before applying the move
            if (room.redTimeMillis <= 0 || room.blackTimeMillis <= 0) {
                room.status = RoomStatus.FINISHED
                return MakeMoveResult(
                    success = true,
                    timedOut = true,
                    redTimeMillis = room.redTimeMillis,
                    blackTimeMillis = room.blackTimeMillis
                )
            }

            // Apply the move to the live board and evaluate game status
            val board = roomBoards[roomId] ?: Board.initial()
            val from = Position(move.fromRow, move.fromCol)
            val to = Position(move.toRow, move.toCol)
            val piece = board[from] ?: run {
                log.warn("[SVC] makeMove: no piece at ({},{}) in room {}", move.fromRow, move.fromCol, roomId)
                return MakeMoveResult(success = false)
            }
            val captured = board[to]
            val newBoard = board.applyMove(Move(from, to, piece, captured))
            roomBoards[roomId] = newBoard

            room.moves.add(move)
            room.currentTurn = if (isRedTurn) "BLACK" else "RED"

            // The player who just moved: if their move leaves the opponent with no valid moves,
            // it's checkmate/stalemate. GameRules evaluates from the NEXT player's perspective.
            val nextColor = if (isRedTurn) PieceColor.BLACK else PieceColor.RED
            val gameStatus = GameRules.getGameStatus(newBoard, nextColor)
            if (gameStatus != GameStatus.PLAYING) {
                room.status = RoomStatus.FINISHED
            }

            return MakeMoveResult(
                success = true,
                gameStatus = gameStatus,
                redTimeMillis = room.redTimeMillis,
                blackTimeMillis = room.blackTimeMillis
            )
        }
    }

    fun resign(roomId: String, playerId: String): String? {
        val room = rooms[roomId] ?: return null
        synchronized(room) {
            if (room.status == RoomStatus.FINISHED) return null
            room.status = RoomStatus.FINISHED
            return if (room.redPlayer?.id == playerId) "black_wins" else "red_wins"
        }
    }

    /**
     * Removes a player from the room. If the game was in progress this is treated as a
     * forfeit: returns the result string for the opponent's win. Returns null forfeit result
     * if the game had not yet started.
     */
    fun abandonGame(roomId: String, playerId: String): Pair<GameRoom, String>? {
        val room = rooms[roomId] ?: return null
        synchronized(room) {
            val wasPlaying = room.status == RoomStatus.PLAYING
            val forfeitResult: String? = if (wasPlaying) {
                if (room.redPlayer?.id == playerId) "black_wins" else "red_wins"
            } else null

            room.status = if (wasPlaying) RoomStatus.FINISHED else RoomStatus.WAITING
            if (forfeitResult == null) return null

            if (room.redPlayer?.id == playerId) room.redPlayer = null
            else if (room.blackPlayer?.id == playerId) room.blackPlayer = null

            return Pair(room, forfeitResult)
        }
    }


    // -------------------------------------------------------------------------
    // Matchmaking
    // -------------------------------------------------------------------------

    fun joinQueue(sessionId: String, player: Player, timeControlSeconds: Int) {
        matchmakingQueue[sessionId] = QueueEntry(player, sessionId, timeControlSeconds)
    }

    fun leaveQueue(sessionId: String) {
        matchmakingQueue.remove(sessionId)
    }

    /**
     * Finds a match for the given session. Uses an atomic remove on the opponent slot to
     * prevent the TOCTOU race where two sessions simultaneously claim each other.
     */
    fun findMatch(sessionId: String): Pair<QueueEntry, QueueEntry>? {
        val entry = matchmakingQueue[sessionId] ?: return null
        val opponent = matchmakingQueue.values.firstOrNull {
            it.sessionId != sessionId && it.timeControlSeconds == entry.timeControlSeconds
        } ?: return null

        // Atomically claim the opponent. If another thread already removed them, bail out.
        val claimedOpponent = matchmakingQueue.remove(opponent.sessionId) ?: return null
        matchmakingQueue.remove(sessionId)
        return entry to claimedOpponent
    }

    fun getQueuePosition(sessionId: String): Int {
        val entries = matchmakingQueue.values.sortedBy { it.joinedAt }
        return entries.indexOfFirst { it.sessionId == sessionId } + 1
    }

    fun getQueueEntry(sessionId: String): QueueEntry? = matchmakingQueue[sessionId]

    fun removeMatchMakingQueue(sessionId: String) {
        matchmakingQueue.remove(sessionId)
    }

    // -------------------------------------------------------------------------
    // Room session tracking
    // -------------------------------------------------------------------------

    fun addRoomSession(roomId: String, sessionId: String) {
        roomSessionIdsMap.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(sessionId)
    }

    fun removeSessionIdFromRoom(roomId: String, sessionId: String) {
        val set = roomSessionIdsMap[roomId] ?: return
        set.remove(sessionId)
        if (set.isEmpty()) roomSessionIdsMap.remove(roomId)
    }

    fun getRoomSessionIds(roomId: String): Set<String> = roomSessionIdsMap[roomId] ?: emptySet()

    fun getRoomIdsForSession(sessionId: String): List<String> =
        roomSessionIdsMap.entries.filter { it.value.contains(sessionId) }.map { it.key }

    // -------------------------------------------------------------------------
    // Bot helpers
    // -------------------------------------------------------------------------


    fun roomHasBot(roomId: String): Boolean {
        val room = rooms[roomId] ?: return false
        return (room.redPlayer?.id?.startsWith("bot-") == true) ||
               (room.blackPlayer?.id?.startsWith("bot-") == true)
    }
}
