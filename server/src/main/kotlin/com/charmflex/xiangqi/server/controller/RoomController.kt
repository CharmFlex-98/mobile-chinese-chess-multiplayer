package com.charmflex.xiangqi.server.controller

import com.charmflex.xiangqi.server.exception.ResourcesNotFound
import com.charmflex.xiangqi.server.exception.UnauthorizedException
import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.service.GameService
import com.charmflex.xiangqi.server.service.JwtValidator
import com.charmflex.xiangqi.server.service.PlayerPersistenceService
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class RoomController(
    private val gameService: GameService,
    private val jwtValidator: JwtValidator,
    private val playerPersistenceService: PlayerPersistenceService
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
        log.info("[API] Supabase player: id={} name={}", player.id.take(8), player.name)

        return ResponseEntity.ok(
            LoginVerifyResponse(
                token = body.token,
                uid = jwtResult.userId,
                displayName = body.displayName,
                guest = false
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

        val room = gameService.createRoom(player, body.name, body.timeControlSeconds, body.isPrivate)
        return ResponseEntity.ok(CreateRoomResponse(roomId = room.id))
    }

    @PostMapping("/rooms/{roomId}/join")
    fun joinRoom(
        @PathVariable roomId: String,
        authentication: Authentication
    ): ResponseEntity<BattleRoomResponse> {
        val jwt = (authentication.principal as? Jwt) ?: throw UnauthorizedException
        val userId = jwt.subject
        val player = playerPersistenceService.findById(userId)?.toPlayer() ?: throw ResourcesNotFound

        val room = gameService.joinRoom(roomId, player)
            ?: run {
                log.warn("[API] Join room FAILED: room={} player={}", roomId, player.name)
                return ResponseEntity.badRequest().build()
            }

        log.info("[API] Joined room: id={} red={} black={} status={}", room.id, room.redPlayer?.name, room.blackPlayer?.name, room.status)
        return ResponseEntity.ok(room.toResponse())
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
        isPrivate = private
    )
}
