package com.charmflex.xiangqi.server.service

import com.charmflex.xiangqi.engine.ai.AiDifficulty
import com.charmflex.xiangqi.engine.ai.AiEngine
import com.charmflex.xiangqi.engine.model.*
import com.charmflex.xiangqi.engine.rules.GameRules
import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.websocket.*
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Service
class BotService(
    private val gameService: GameService,
    private val orchestrator: GameOrchestrator,
    private val sessionRegistry: SessionRegistry,
    private val playerPersistenceService: PlayerPersistenceService
) {
    private val log = LoggerFactory.getLogger(BotService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Bot pool — initialized eagerly then synced with DB in @PostConstruct
    private var botPool: List<BotPlayer> = createBotPool()

    // Daily game counters: bot name -> DailyCounter
    private data class DailyCounter(val date: LocalDate, val count: Int)

    private val botDailyCounts = ConcurrentHashMap<String, DailyCounter>()

    // Active bot game loops: roomId -> Job
    private val activeBotGames = ConcurrentHashMap<String, Job>()

    // Bot boards: roomId -> Board (mirrors the game state for AI computation)
    private val botBoards = ConcurrentHashMap<String, Board>()

    // Bot color in each room: roomId -> PieceColor
    private val botColors = ConcurrentHashMap<String, PieceColor>()

    // Bot player in each room: roomId -> BotPlayer
    private val botPlayers = ConcurrentHashMap<String, BotPlayer>()

    // Pending bot matchmaking timers: sessionId -> Job
    private val pendingBotMatches = ConcurrentHashMap<String, Job>()

    // Track bot-vs-bot rooms: roomId -> (redBot, blackBot)
    private val botVsBotRooms = ConcurrentHashMap<String, Pair<BotPlayer, BotPlayer>>()

    private fun createBotPool(): List<BotPlayer> {
        data class BotDef(val name: String, val rating: Int, val difficulty: AiDifficulty)

        val defs = listOf(
            // BEGINNER (5)
            BotDef("Aiman Rahman", 0, AiDifficulty.BEGINNER),
            BotDef("Priya Nair", 0, AiDifficulty.BEGINNER),
            BotDef("Lucas Martin", 0, AiDifficulty.BEGINNER),
            BotDef("Nurul Izzah", 0, AiDifficulty.BEGINNER),
            BotDef("Daniel Smith", 0, AiDifficulty.BEGINNER),

// EASY (8)
            BotDef("Farhan Ali", 0, AiDifficulty.EASY),
            BotDef("Anjali Kumar", 0, AiDifficulty.EASY),
            BotDef("Jason Miller", 0, AiDifficulty.EASY),
            BotDef("Mei Ling Tan", 0, AiDifficulty.EASY),
            BotDef("Adam Walker", 0, AiDifficulty.EASY),
            BotDef("Siti Aisyah", 0, AiDifficulty.EASY),
            BotDef("Rajesh Pillai", 0, AiDifficulty.EASY),
            BotDef("Hannah Brown", 0, AiDifficulty.EASY),

// MEDIUM (10)
            BotDef("Zachary Lee", 0, AiDifficulty.MEDIUM),
            BotDef("Alicia Fernandez", 0, AiDifficulty.MEDIUM),
            BotDef("Harith Iskandar", 0, AiDifficulty.MEDIUM),
            BotDef("Kavitha Raman", 0, AiDifficulty.MEDIUM),
            BotDef("Ethan Johnson", 0, AiDifficulty.MEDIUM),
            BotDef("Syafiq Azman", 0, AiDifficulty.MEDIUM),
            BotDef("Isabella Rossi", 0, AiDifficulty.MEDIUM),
            BotDef("Wei Jian Ong", 0, AiDifficulty.MEDIUM),
            BotDef("Ryan Patel", 0, AiDifficulty.MEDIUM),
            BotDef("Clara Schmidt", 0, AiDifficulty.MEDIUM),

// INTERMEDIATE (15)
            BotDef("Muhammad Firdaus", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Arjun Menon", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Sophia Williams", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Jonathan Clark", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Nur Farah", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Benjamin Tan", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Amirah Zainal", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Marcus Robinson", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Devi Krishnan", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Caleb Anderson", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Irfan Hakim", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Natalie Garcia", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Daniel O'Connor", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Nadia Hussein", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Samuel Kim", 0, AiDifficulty.INTERMEDIATE),

// HARD (8)
            BotDef("Christopher Evans", 0, AiDifficulty.HARD),
            BotDef("Ravi Subramaniam", 0, AiDifficulty.HARD),
            BotDef("Ahmad Danish", 0, AiDifficulty.HARD),
            BotDef("Victor Ivanov", 0, AiDifficulty.HARD),
            BotDef("Leonardo Costa", 0, AiDifficulty.HARD),
            BotDef("Mohd Faizal", 0, AiDifficulty.HARD),
            BotDef("Andrew Thompson", 0, AiDifficulty.HARD),
            BotDef("Prakash Singh", 0, AiDifficulty.HARD),

// EXPERT (4)
            BotDef("Alexander Petrov", 0, AiDifficulty.EXPERT),
            BotDef("Hiroshi Tanaka", 0, AiDifficulty.EXPERT),
            BotDef("Omar Al-Farsi", 0, AiDifficulty.EXPERT),
            BotDef("William Carter", 0, AiDifficulty.EXPERT),

            // BEGINNER (4)
            BotDef("Aisyah Rahim", 0, AiDifficulty.BEGINNER),
            BotDef("Kelvin Chua", 0, AiDifficulty.BEGINNER),
            BotDef("Naveen Kumar", 0, AiDifficulty.BEGINNER),
            BotDef("Emily Wong", 0, AiDifficulty.BEGINNER),

// EASY (8)
            BotDef("Farid Hakimi", 0, AiDifficulty.EASY),
            BotDef("Vanessa Lim", 0, AiDifficulty.EASY),
            BotDef("Gopal Nair", 0, AiDifficulty.EASY),
            BotDef("Sharon Lee", 0, AiDifficulty.EASY),
            BotDef("Daniel Tan", 0, AiDifficulty.EASY),
            BotDef("Aiman Zulkifli", 0, AiDifficulty.EASY),
            BotDef("Rashmi Iyer", 0, AiDifficulty.EASY),
            BotDef("Kevin Loh", 0, AiDifficulty.EASY),

// MEDIUM (12)
            BotDef("Marcus Lim", 0, AiDifficulty.MEDIUM),
            BotDef("Hafiz Rahman", 0, AiDifficulty.MEDIUM),
            BotDef("Jessica Tan", 0, AiDifficulty.MEDIUM),
            BotDef("Arvind Rao", 0, AiDifficulty.MEDIUM),
            BotDef("Samuel Wong", 0, AiDifficulty.MEDIUM),
            BotDef("Chloe Martin", 0, AiDifficulty.MEDIUM),
            BotDef("Izzat Fadhil", 0, AiDifficulty.MEDIUM),
            BotDef("Nathaniel Goh", 0, AiDifficulty.MEDIUM),
            BotDef("Deepa Krishnan", 0, AiDifficulty.MEDIUM),
            BotDef("Jonathan Lim", 0, AiDifficulty.MEDIUM),
            BotDef("Mei Xin Lau", 0, AiDifficulty.MEDIUM),
            BotDef("Ryan Abdullah", 0, AiDifficulty.MEDIUM),

// INTERMEDIATE (14)
            BotDef("Firdaus Khalid", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Adrian Tan", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Samantha Lee", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Zul Hilmi", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Benjamin Lee", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Alicia Wong", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Haroon Siddiq", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Rachel Lim", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Vikram Nair", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Jason Ong", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Farah Nabila", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Nicholas Teo", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Priyanka Das", 0, AiDifficulty.INTERMEDIATE),
            BotDef("Daniel Yap", 0, AiDifficulty.INTERMEDIATE),

// HARD (8)
            BotDef("Alexander Lim", 0, AiDifficulty.HARD),
            BotDef("Rizwan Ahmad", 0, AiDifficulty.HARD),
            BotDef("Leon Tan", 0, AiDifficulty.HARD),
            BotDef("Wei Zhong Chen", 0, AiDifficulty.HARD),
            BotDef("Arjun Pillai", 0, AiDifficulty.HARD),
            BotDef("Sofia Martinez", 0, AiDifficulty.HARD),
            BotDef("Karthik Menon", 0, AiDifficulty.HARD),
            BotDef("David Ng", 0, AiDifficulty.HARD),

// EXPERT (4)
            BotDef("Ivan Volkov", 0, AiDifficulty.EXPERT),
            BotDef("Kenji Nakamura", 0, AiDifficulty.EXPERT),
            BotDef("Ahmad Zulfikar", 0, AiDifficulty.EXPERT),
            BotDef("Sebastian Müller", 0, AiDifficulty.EXPERT),

            )

        return defs.map { def ->
            BotPlayer(
                player = Player(
                    id = "bot-${def.name}",
                    name = def.name,
                    xp = def.rating,
                    level = Player.computeLevel(def.rating)
                ),
                difficulty = def.difficulty,
                minDelayMs = when {
                    def.rating < 1000 -> 3000L
                    def.rating < 1400 -> 2000L
                    else -> 1500L
                },
                maxDelayMs = when {
                    def.rating < 1000 -> 10000L
                    def.rating < 1400 -> 8000L
                    else -> 6000L
                }
            )
        }
    }

    // --- Bot Matchmaking ---
    // TODO: if bot and real player pick up at the same time?
    fun onPlayerQueueJoin(sessionId: String, timeControlSeconds: Int) {
        val job = scope.launch {
            val waitMs = (10_000L..15_000L).random()
            delay(waitMs)
            if (!isActive) return@launch

            // Check if player is still in the queue
            val queueEntry = gameService.getQueueEntry(sessionId) ?: return@launch

            val bot = pickBotForLevel(Player.computeLevel(queueEntry.player.xp))

            // Create room with real player as RED, bot as BLACK
            val room = gameService.createRoom(queueEntry.player, "Matched Game", timeControlSeconds, false)
            gameService.joinRoom(room.id, bot.player)
            gameService.leaveQueue(sessionId)

            val botColor = PieceColor.BLACK

            // Register real player's session in the room
            gameService.addRoomSession(room.id, sessionId)

            // Send match_found to real player
            val matchMsg = WsMessageBuilder.buildGameMessage(
                WsType.MATCH_FOUND,
                MatchFoundPayload(
                    roomId = room.id,
                    opponent = bot.player,
                    playerColor = "RED"
                )
            )
            sessionRegistry.sendToSession(sessionId, matchMsg)

            // Increment daily counter for the chosen bot
            incrementBotDailyCountByName(bot.player.name)

            // Remove completed job from the map so it doesn't accumulate indefinitely
            pendingBotMatches.remove(sessionId)

            // Start bot game loop
            startBotGame(room.id, bot, botColor)
        }
        pendingBotMatches[sessionId] = job
    }

    fun onPlayerQueueLeave(sessionId: String) {
        pendingBotMatches.remove(sessionId)?.cancel()
    }

    // --- Bot Game Loop ---

    fun startBotGame(roomId: String, bot: BotPlayer, botColor: PieceColor) {
        val board = Board.initial()
        botBoards[roomId] = board
        botColors[roomId] = botColor
        botPlayers[roomId] = bot

        // If bot plays RED (first move), trigger immediately
        if (botColor == PieceColor.RED) {
            scheduleBotMove(roomId)
        }
    }

    fun onOpponentMove(roomId: String, move: MoveDto) {
        val board = botBoards[roomId] ?: return
        val botColor = botColors[roomId] ?: return

        // Apply opponent's move to local board
        val from = Position(move.fromRow, move.fromCol)
        val to = Position(move.toRow, move.toCol)
        val piece = board[from] ?: return
        val captured = board[to]
        val engineMove = Move(from, to, piece, captured)
        val newBoard = board.applyMove(engineMove)
        botBoards[roomId] = newBoard

        // Safety check: if game is already over from this move (should have been caught by
        // handleMakeMove, but guard here for correctness), skip scheduling.
        val status = GameRules.getGameStatus(newBoard, botColor)
        if (status != GameStatus.PLAYING) {
            cleanupBotGame(roomId)
            return
        }

        // Schedule bot's response
        scheduleBotMove(roomId)
    }

    private fun scheduleBotMove(roomId: String) {
        val bot = botPlayers[roomId] ?: return
        val botColor = botColors[roomId] ?: return

        activeBotGames.remove(roomId)?.cancel()
        val job = scope.launch {
            try {
                val delayMs = (bot.minDelayMs..bot.maxDelayMs).random()
                delay(delayMs)
                if (!isActive) return@launch

                val board = botBoards[roomId] ?: run { cleanupBotGame(roomId); return@launch }
                val room = gameService.getRoom(roomId) ?: run { cleanupBotGame(roomId); return@launch }
                if (room.status != RoomStatus.PLAYING) { cleanupBotGame(roomId); return@launch }

                // Compute move
                val engine = AiEngine(bot.difficulty)
                val bestMove = engine.findBestMove(board, botColor)
                if (bestMove == null) {
                    log.warn("[BOT] No move found for bot {} in room {}", bot.player.name, roomId)
                    activeBotGames.remove(roomId) // free the slot; stale-sweep will end the room
                    return@launch
                }

                // Apply move to local board
                val newBoard = board.applyMove(bestMove)
                botBoards[roomId] = newBoard

                // Delegate to orchestrator: applies move, broadcasts, handles game-over + XP + cleanup
                val moveDto = MoveDto(bestMove.from.row, bestMove.from.col, bestMove.to.row, bestMove.to.col)
                val moveResult = orchestrator.applyBotMove(roomId, moveDto, bot.player.id) ?: run {
                    log.warn("[BOT] Move rejected for room {}, bot={} — ending game", roomId, bot.player.name)
                    botBoards[roomId] = board // revert local board
                    gameService.finishGame(roomId)
                    cleanupBotGame(roomId)
                    return@launch
                }

                // For bot-vs-bot games, trigger the next bot's move only if game is still ongoing
                val gameStillRunning = moveResult.gameStatus == GameStatus.PLAYING && !moveResult.timedOut
                if (isBotVsBotGame(roomId) && gameStillRunning) {
                    val bots = botVsBotRooms[roomId] ?: return@launch
                    val opponentColor = botColor.opponent
                    val nextBot = if (opponentColor == PieceColor.RED) bots.first else bots.second
                    botColors[roomId] = opponentColor
                    botPlayers[roomId] = nextBot
                    scheduleBotMove(roomId)
                }
            } catch (e: Exception) {
                log.error("[BOT] Unexpected error in bot loop for room {}, cleaning up", roomId, e)
                gameService.finishGame(roomId)
                cleanupBotGame(roomId)
            }
        }
        activeBotGames[roomId] = job
    }

    // --- Daily limit helpers ---

    private fun canBotPlayByName(name: String): Boolean {
        val today = LocalDate.now()
        val counter = botDailyCounts[name] ?: return true
        if (counter.date != today) return true
        return counter.count < 5
    }

    private fun incrementBotDailyCountByName(name: String) {
        botDailyCounts.compute(name) { _, existing ->
            val today = LocalDate.now()
            if (existing == null || existing.date != today) {
                DailyCounter(today, 1)
            } else {
                existing.copy(count = existing.count + 1)
            }
        }
    }

    // --- Bot-vs-Bot Lobby Simulation ---

    @PostConstruct
    fun initializeService() {
        ensureBotsRegistered()
        startLobbySimulation()
    }

    private fun ensureBotsRegistered() {
        log.info("[BOT] Persisting {} bots to DB if absent", botPool.size)
        botPool = botPool.map { bot ->
            val dbPlayer = playerPersistenceService.persistBotIfAbsent(
                bot.player.id, bot.player.name, bot.player.xp
            )
            bot.copy(player = dbPlayer)
        }
    }

    private fun startLobbySimulation() {
        log.info("[BOT] Starting lobby simulation")
        scope.launch {
            delay(5_000)

            while (isActive) {
                // Sweep: clean up botVsBotRooms entries whose room is gone or no longer PLAYING
                botVsBotRooms.keys.toList().forEach { roomId ->
                    val room = gameService.getRoom(roomId)
                    if (room == null || room.status != RoomStatus.PLAYING) {
                        log.info("[BOT] Sweeping stale bot-vs-bot room {}", roomId)
                        cleanupBotGame(roomId)
                    }
                }

                val activeCount = botVsBotRooms.keys.count { roomId ->
                    gameService.getRoom(roomId)?.status == RoomStatus.PLAYING
                }

                val targetGames = (5..10).random()
                if (activeCount < targetGames) {
                    repeat(targetGames - activeCount) {
                        createBotVsBotGame()
                    }
                }

                // Delay 15-30 minutes and recreate the bot
                delay((90_000L..180_0000L).random())
            }
        }
    }

    fun idleBots(): List<BotPlayer> {
        // Bots in bot-vs-bot: both red and black are stored in botVsBotRooms
        val botVsBotIds = botVsBotRooms.values
            .flatMap { (red, black) -> listOf(red.player.id, black.player.id) }
            .toSet()
        // Bots in bot-vs-human: stored in botPlayers for rooms NOT in botVsBotRooms
        // (bot-vs-bot rooms also have botPlayers[roomId] = current-turn bot, already covered above)
        val botVsHumanIds = botPlayers.entries
            .filter { (roomId, _) -> !botVsBotRooms.containsKey(roomId) }
            .map { it.value.player.id }
            .toSet()
        val activeBotIds = botVsBotIds + botVsHumanIds
        return botPool.filter { it.player.id !in activeBotIds }
    }

    private fun createBotVsBotGame() {
        val availableBots = idleBots().shuffled()
        if (availableBots.size < 2) return

        // Skip if not enough bots under daily limit
        val botsUnderLimit = availableBots.filter { canBotPlayByName(it.player.name) }
        if (botsUnderLimit.size < 2) {
            return
        }

        val redBot = botsUnderLimit[0].copy(minDelayMs = 5000L, maxDelayMs = 20000L)
        val blackBot = botsUnderLimit[1].copy(minDelayMs = 5000L, maxDelayMs = 20000L)

        val lobbyRedBot = redBot
        val lobbyBlackBot = blackBot

        incrementBotDailyCountByName(redBot.player.name)
        incrementBotDailyCountByName(blackBot.player.name)

        val room = gameService.createRoom(lobbyRedBot.player, "Matched Game", 1800, false)
        gameService.joinRoom(room.id, lobbyBlackBot.player)
        gameService.recordGameStart(room.id)

        val board = Board.initial()
        botBoards[room.id] = board
        botColors[room.id] = PieceColor.RED
        botPlayers[room.id] = lobbyRedBot

        botVsBotRooms[room.id] = lobbyRedBot to lobbyBlackBot

        scheduleBotMove(room.id)
    }

    private fun isBotVsBotGame(roomId: String): Boolean = botVsBotRooms.containsKey(roomId)

    // --- Helpers ---

    private fun pickBotForLevel(playerLevel: Int): BotPlayer {
        val targetDifficulty = when {
            playerLevel <= 3 -> AiDifficulty.EASY
            playerLevel <= 15 -> AiDifficulty.MEDIUM
            playerLevel <= 25 -> AiDifficulty.INTERMEDIATE
            playerLevel <= 35 -> AiDifficulty.EXPERT
            playerLevel <= 45 -> AiDifficulty.MASTER
            playerLevel <= 55 -> AiDifficulty.GRANDMASTER
            else -> AiDifficulty.LEGEND
        }
        val candidates = idleBots().filter { it.difficulty == targetDifficulty }.ifEmpty { botPool }
        // Prefer bots under daily limit; fall back to any available bot if all at limit
        val underLimit = candidates.filter { canBotPlayByName(it.player.name) }
        return (underLimit.ifEmpty { candidates }).random()
    }

    fun pauseBotGame(roomId: String) {
        activeBotGames.remove(roomId)?.cancel()
        // botBoards / botColors / botPlayers stay intact for resume
    }

    fun resumeBotGame(roomId: String) {
        val room = gameService.getRoom(roomId) ?: return
        val botColor = botColors[roomId] ?: run {
            log.warn("[BOT] resumeBotGame: botColors missing for room {} — bot maps were cleaned up, skipping", roomId)
            return
        }
        val currentTurnColor = if (room.currentTurn == "RED") PieceColor.RED else PieceColor.BLACK
        if (currentTurnColor == botColor) {
            scheduleBotMove(roomId)
        }
    }

    fun onGameOver(roomId: String) {
        cleanupBotGame(roomId)
    }

    private fun cleanupBotGame(roomId: String) {
        activeBotGames.remove(roomId)?.cancel()
        botBoards.remove(roomId)
        botColors.remove(roomId)
        botPlayers.remove(roomId)
        botVsBotRooms.remove(roomId)
    }

    fun isBotSession(sessionId: String): Boolean = sessionId.startsWith("bot-")

    fun hasBot(roomId: String): Boolean = botPlayers.containsKey(roomId)

    fun updateBotInPool(updatedPlayer: Player) {
        botPool = botPool.map {
            if (it.player.id == updatedPlayer.id) it.copy(player = updatedPlayer) else it
        }
    }
}
