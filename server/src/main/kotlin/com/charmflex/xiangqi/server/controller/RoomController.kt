package com.charmflex.xiangqi.server.controller

import com.charmflex.xiangqi.server.exception.UnauthorizedException
import com.charmflex.xiangqi.server.model.*
import com.charmflex.xiangqi.server.service.GameService
import com.charmflex.xiangqi.server.service.JwtValidator
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class RoomController(
    private val gameService: GameService,
    private val jwtValidator: JwtValidator
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
        val tokenUserId = jwtValidator.validateAndGetUserId(body.token)
        if (tokenUserId == null || tokenUserId != body.uid) {
            throw UnauthorizedException
        }

        log.info("[API] Supabase login: userId={} name={}", body.uid.take(8), body.displayName)
        val player = gameService.getOrCreatePlayer(body.uid, body.displayName)
        log.info("[API] Supabase player: id={} name={}", player.id.take(8), player.name)

        return ResponseEntity.ok(
            LoginVerifyResponse(
                token = body.token,
                uid = tokenUserId,
                displayName = body.displayName,
                guest = false
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
        @RequestHeader("Authorization", required = false) auth: String?,
        @RequestBody body: CreateRoomRequest
    ): ResponseEntity<CreateRoomResponse> {
        val token = extractToken(auth)
        log.info("[API] POST /rooms token={}", token?.take(8))
        val player = token?.let { gameService.getPlayerById(it) }
            ?: run {
                log.warn("[API] Create room: invalid player token={}", token?.take(8))
                return ResponseEntity.badRequest().build()
            }

        val room = gameService.createRoom(player, body.name, body.timeControlSeconds, body.isPrivate)
        log.info("[API] Room created: id={} name={} by={}", room.id, body.name, player.name)
        return ResponseEntity.ok(CreateRoomResponse(roomId = room.id))
    }

    @PostMapping("/rooms/{roomId}/join")
    fun joinRoom(
        @PathVariable roomId: String,
        @RequestHeader("Authorization", required = false) auth: String?
    ): ResponseEntity<BattleRoomResponse> {
        val token = extractToken(auth)
        log.info("[API] POST /rooms/{}/join token={}", roomId, token?.take(8))
        val player = token?.let { gameService.getPlayerById(it) }
            ?: run {
                log.warn("[API] Join room: invalid player token={}", token?.take(8))
                return ResponseEntity.badRequest().build()
            }

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
        host = redPlayer ?: Player(name = "Unknown"),
        guest = blackPlayer,
        status = status.name.lowercase(),
        timeControlSeconds = timeControlSeconds,
        isPrivate = private
    )
}
