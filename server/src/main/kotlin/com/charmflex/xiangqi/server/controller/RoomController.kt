package com.charmflex.xiangqi.server.controller

import com.charmflex.xiangqi.server.exception.ResourcesNotFound
import com.charmflex.xiangqi.server.exception.UnauthorizedException
import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.service.DiscordService
import com.charmflex.xiangqi.server.service.GameOrchestrator
import com.charmflex.xiangqi.server.service.GameService
import com.charmflex.xiangqi.server.service.JwtValidator
import com.charmflex.xiangqi.server.service.PlayerPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class RoomController(
    private val gameService: GameService,
    private val jwtValidator: JwtValidator,
    private val playerPersistenceService: PlayerPersistenceService,
    private val discordService: DiscordService,
    private val gameOrchestrator: GameOrchestrator,
    @Value("\${admin.player-ids}") private val adminPlayerIds: String
) {
    private val log = LoggerFactory.getLogger(RoomController::class.java)

    @PostMapping("/auth/guest")
    fun guestLogin(@RequestBody body: GuestLoginRequest): ResponseEntity<AuthResponse> {
        val name = body.username.ifBlank { body.displayName.ifBlank { "Guest" } }
        log.info("[API] POST /auth/guest name={}", name)
        val player = gameService.createGuestPlayer(name)
        log.info("[API] Guest created: id={} name={}", player.id.take(8), player.name)
        return ResponseEntity.ok(AuthResponse(token = player.id, player = player))
    }

    @PostMapping("/auth/login/verify")
    fun loginVerify(@RequestBody body: LoginVerifyRequest): ResponseEntity<LoginVerifyResponse> {
        val jwtResult = jwtValidator.validateAndGetUserId(body.token)
        if (jwtResult == null || jwtResult.userId != body.uid) {
            throw UnauthorizedException
        }

        log.info("[API] Supabase login: userId={} name={}", body.uid.take(8), body.displayName)
        val player = playerPersistenceService.getOrCreatePlayer(body.uid, body.displayName)
        val isAdmin = isAdmin(jwtResult.userId)
        log.info("[API] Supabase player: id={} name={} xp={} level={} admin={}", player.id.take(8), player.name, player.xp, player.level, isAdmin)

        return ResponseEntity.ok(
            LoginVerifyResponse(
                token = body.token,
                uid = jwtResult.userId,
                displayName = body.displayName,
                guest = false,
                xp = player.xp,
                level = player.level,
                admin = isAdmin
            )
        )
    }

    @GetMapping("/me/game")
    fun getMyActiveGame(authentication: Authentication): ResponseEntity<ActiveGameResponse> {
        val jwt = (authentication.principal as? Jwt) ?: throw UnauthorizedException
        val userId = jwt.subject
        val room = gameService.findActiveRoomForPlayer(userId)
            ?: return ResponseEntity.notFound().build()
        val playerColor = if (room.redPlayer?.id == userId) "RED" else "BLACK"
        val opponentName = if (playerColor == "RED") room.blackPlayer?.name ?: "Opponent"
                           else room.redPlayer?.name ?: "Opponent"
        log.info("[API] GET /me/game -> room={} player={} color={}", room.id, userId.take(8), playerColor)
        return ResponseEntity.ok(
            ActiveGameResponse(
                roomId = room.id,
                opponentName = opponentName,
                playerColor = playerColor,
                redTimeMillis = room.redTimeMillis,
                blackTimeMillis = room.blackTimeMillis
            )
        )
    }

    @GetMapping("/public/leaderboard")
    fun getLeaderboard(): ResponseEntity<LeaderboardResponse> {
        val players = playerPersistenceService.getTopPlayersByXp()
        val entries = players.mapIndexed { _, entity ->
            LeaderboardEntry(name = entity.name, xp = entity.xp, level = entity.level)
        }
        log.info("[API] GET /public/leaderboard -> {} entries", entries.size)
        return ResponseEntity.ok(LeaderboardResponse(entries = entries))
    }

    @GetMapping("/rooms")
    fun getActiveRooms(): ResponseEntity<ActiveRoomsResponse> {
        val rooms = gameService.getActiveRooms().map { it.toResponse() }
        log.info("[API] GET /rooms -> {} rooms", rooms.size)
        return ResponseEntity.ok(ActiveRoomsResponse(rooms = rooms))
    }

    @PostMapping("/rooms/create")
    fun createRoom(
        @RequestBody body: CreateRoomRequest,
        authentication: Authentication
    ): ResponseEntity<CreateRoomResponse> {
        val jwt = (authentication.principal as? Jwt) ?: throw UnauthorizedException
        val userId = jwt.subject
        val player = playerPersistenceService.findById(userId)?.toPlayer() ?: throw ResourcesNotFound

        val room = gameService.createRoom(player, body.name, body.timeControlSeconds, body.isPrivate, body.password)
        return ResponseEntity.ok(CreateRoomResponse(roomId = room.id))
    }

    @PostMapping("/rooms/{roomId}/join")
    fun joinRoom(
        @PathVariable roomId: String,
        @RequestBody(required = false) body: JoinRoomRequest? = null,
        authentication: Authentication
    ): ResponseEntity<BattleRoomResponse> {
        val jwt = (authentication.principal as? Jwt) ?: throw UnauthorizedException
        val userId = jwt.subject
        val player = playerPersistenceService.findById(userId)?.toPlayer() ?: throw ResourcesNotFound

        if (!gameService.validateRoomPassword(roomId, body?.password)) {
            log.warn("[API] Join room REJECTED (wrong password): room={} player={}", roomId, player.name)
            return ResponseEntity.status(403).build()
        }

        val room = gameService.joinRoom(roomId, player)
            ?: run {
                log.warn("[API] Join room FAILED: room={} player={}", roomId, player.name)
                return ResponseEntity.badRequest().build()
            }

        log.info("[API] Joined room: id={} red={} black={} status={}", room.id, room.redPlayer?.name, room.blackPlayer?.name, room.status)
        return ResponseEntity.ok(room.toResponse())
    }

    @PostMapping("/admin/rooms/{roomId}/chat")
    fun adminChat(
        @PathVariable roomId: String,
        @RequestBody body: AdminChatRequest,
        authentication: Authentication
    ): ResponseEntity<Unit> {
        val jwt = (authentication.principal as? Jwt) ?: throw UnauthorizedException
        if (!isAdmin(jwt.subject)) throw UnauthorizedException
        val sent = gameOrchestrator.handleAdminChat(roomId, body.message)
        return if (sent) ResponseEntity.ok().build() else ResponseEntity.notFound().build()
    }

    private fun isAdmin(userId: String): Boolean {
        if (adminPlayerIds.isBlank()) return false
        return adminPlayerIds.split(",").any { it.trim() == userId }
    }

    private fun extractToken(auth: String?): String? {
        if (auth == null) return null
        return if (auth.startsWith("Bearer ")) auth.removePrefix("Bearer ") else auth
    }

    private fun GameRoom.toResponse(): BattleRoomResponse = BattleRoomResponse(
        id = id,
        name = name,
        host = redPlayer,
        guest = blackPlayer,
        status = status.name.lowercase(),
        timeControlSeconds = timeControlSeconds,
        isPrivate = private,
        hasPassword = password != null
    )
}
