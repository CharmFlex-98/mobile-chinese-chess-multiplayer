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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class BotService(
    private val gameService: GameService,
    private val orchestrator: GameOrchestrator,
    private val sessionRegistry: SessionRegistry
) {
    private val log = LoggerFactory.getLogger(BotService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Bot pool
    private val botPool: List<BotPlayer> = createBotPool()

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
            BotDef("棋仙", 1800, AiDifficulty.EXPERT),
            BotDef("竹林高手", 1600, AiDifficulty.HARD),
            BotDef("弈者无敌", 1700, AiDifficulty.EXPERT),
            BotDef("天涯棋客", 1500, AiDifficulty.HARD),
            BotDef("象棋迷", 1300, AiDifficulty.INTERMEDIATE),
            BotDef("小明", 1000, AiDifficulty.MEDIUM),
            BotDef("棋乐无穷", 1400, AiDifficulty.INTERMEDIATE),
            BotDef("将军令", 1600, AiDifficulty.HARD),
            BotDef("红袍将", 1200, AiDifficulty.MEDIUM),
            BotDef("黑马骑士", 1100, AiDifficulty.MEDIUM),
            BotDef("炮轰天下", 1500, AiDifficulty.HARD),
            BotDef("车马炮", 900, AiDifficulty.EASY),
            BotDef("新手上路", 800, AiDifficulty.BEGINNER),
            BotDef("棋坛新秀", 1000, AiDifficulty.MEDIUM),
            BotDef("老将出马", 1700, AiDifficulty.EXPERT)
        )

        return defs.map { def ->
            BotPlayer(
                player = Player(
                    id = "bot-${UUID.randomUUID()}",
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
        log.info("[BOT] Player {} joined queue, starting bot match timer", sessionId)
        val job = scope.launch {
            val waitMs = (10_000L..15_000L).random()
            delay(waitMs)
            if (!isActive) return@launch

            // Check if player is still in the queue
            val queueEntry = gameService.getQueueEntry(sessionId) ?: run {
                log.info("[BOT] Player {} no longer in queue, cancelling bot match", sessionId)
                return@launch
            }

            log.info("[BOT] Creating bot opponent for player {}", queueEntry.player.name)
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

            // Start bot game loop
            startBotGame(room.id, bot, botColor)
        }
        pendingBotMatches[sessionId] = job
    }

    fun onPlayerQueueLeave(sessionId: String) {
        log.info("[BOT] Player {} left queue, cancelling bot match timer", sessionId)
        pendingBotMatches.remove(sessionId)?.cancel()
    }

    // --- Bot Game Loop ---

    fun startBotGame(roomId: String, bot: BotPlayer, botColor: PieceColor) {
        log.info("[BOT] Starting bot game: room={} bot={} color={}", roomId, bot.player.name, botColor)
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
            log.info("[BOT] Game over in room {} after opponent move: {} (should have been caught upstream)", roomId, status)
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
            val delayMs = (bot.minDelayMs..bot.maxDelayMs).random()
            delay(delayMs)
            if (!isActive) return@launch

            val board = botBoards[roomId] ?: return@launch
            val room = gameService.getRoom(roomId) ?: return@launch
            if (room.status != RoomStatus.PLAYING) return@launch

            // Compute move
            val engine = AiEngine(bot.difficulty)
            val bestMove = engine.findBestMove(board, botColor)
            if (bestMove == null) {
                log.warn("[BOT] No move found for bot {} in room {}", bot.player.name, roomId)
                return@launch
            }

            log.info("[BOT] Bot {} plays ({},{})->({},{}) in room {}",
                bot.player.name, bestMove.from.row, bestMove.from.col,
                bestMove.to.row, bestMove.to.col, roomId)

            // Apply move to local board
            val newBoard = board.applyMove(bestMove)
            botBoards[roomId] = newBoard

            // Delegate to orchestrator: applies move, broadcasts, handles game-over + XP + cleanup
            val moveDto = MoveDto(bestMove.from.row, bestMove.from.col, bestMove.to.row, bestMove.to.col)
            val moveResult = orchestrator.applyBotMove(roomId, moveDto, bot.player.id) ?: return@launch

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
        }
        activeBotGames[roomId] = job
    }

    // --- Bot-vs-Bot Lobby Simulation ---

    @PostConstruct
    fun startLobbySimulation() {
        log.info("[BOT] Starting lobby simulation")
        scope.launch {
            delay(5_000)

            while (isActive) {
                val activeCount = activeBotGames.count { (roomId, job) ->
                    job.isActive && isBotVsBotGame(roomId)
                }

                val targetGames = 2
                if (activeCount < targetGames) {
                    repeat(targetGames - activeCount) {
                        createBotVsBotGame()
                    }
                }

                delay((30_000L..90_000L).random())
            }
        }
    }

    private fun createBotVsBotGame() {
        val availableBots = botPool.shuffled()
        if (availableBots.size < 2) return

        val redBot = availableBots[0].copy(
            player = availableBots[0].player.copy(id = "bot-${UUID.randomUUID()}"),
            minDelayMs = 5000L,
            maxDelayMs = 20000L
        )
        val blackBot = availableBots[1].copy(
            player = availableBots[1].player.copy(id = "bot-${UUID.randomUUID()}"),
            minDelayMs = 5000L,
            maxDelayMs = 20000L
        )

        val lobbyRedBot = redBot.copy(difficulty = AiDifficulty.EASY)
        val lobbyBlackBot = blackBot.copy(difficulty = AiDifficulty.EASY)

        val room = gameService.createRoom(lobbyRedBot.player, "Bot Match", 600, false)
        gameService.joinRoom(room.id, lobbyBlackBot.player)
        gameService.recordGameStart(room.id)

        log.info("[BOT] Created bot-vs-bot game: room={} {} vs {}",
            room.id, lobbyRedBot.player.name, lobbyBlackBot.player.name)

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
            playerLevel <= 2 -> AiDifficulty.BEGINNER
            playerLevel <= 4 -> AiDifficulty.EASY
            playerLevel <= 6 -> AiDifficulty.MEDIUM
            playerLevel <= 8 -> AiDifficulty.INTERMEDIATE
            playerLevel <= 10 -> AiDifficulty.HARD
            else -> AiDifficulty.EXPERT
        }
        val candidates = botPool.filter { it.difficulty == targetDifficulty }
        val bot = (candidates.ifEmpty { botPool }).random()
        return bot.copy(player = bot.player.copy(id = "bot-${UUID.randomUUID()}"))
    }

    fun onGameOver(roomId: String) {
        log.info("[BOT] Game over notification for room {}", roomId)
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
}
