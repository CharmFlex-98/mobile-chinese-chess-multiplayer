package com.charmflex.xiangqi.server.controller

import com.charmflex.xiangqi.server.exception.UnauthorizedException
import com.charmflex.xiangqi.server.model.GameRoom
import com.charmflex.xiangqi.server.model.LoginVerifyRequest
import com.charmflex.xiangqi.server.model.LoginVerifyResponse
import com.charmflex.xiangqi.server.service.GameService
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val json = Json { encodeDefaults = true }

@RestController
@RequestMapping("/api")
class RoomController(
    private val gameService: GameService,
    private val jwtValidator: com.charmflex.xiangqi.server.service.JwtValidator
) {
    private val log = LoggerFactory.getLogger(RoomController::class.java)

    @PostMapping("/auth/guest")
    fun guestLogin(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val name = body["username"] ?: "Guest"
        log.info("[API] POST /auth/guest name={}", name)
        val player = gameService.createGuestPlayer(name)
        log.info("[API] Guest created: id={} name={}", player.id.take(8), player.name)
        return ResponseEntity.ok(mapOf(
            "token" to player.id,
            "player" to mapOf(
                "id" to player.id,
                "name" to player.name,
                "xp" to player.xp
            )
        ))
    }

    @PostMapping("/auth/login/verify")
    fun loginVerify(
        @RequestBody body: LoginVerifyRequest,
    ): ResponseEntity<LoginVerifyResponse> {
        val token = body.token
        val tokenUserId = jwtValidator.validateAndGetUserId(token)
        if (tokenUserId == null || tokenUserId != body.uid) {
            throw UnauthorizedException
        }

        val displayName = body.displayName
        log.info("[API] Supabase login: userId={} name={}", body.uid.take(8), displayName)

        val player = gameService.getOrCreatePlayer(body.uid, displayName)
        log.info("[API] Supabase player: id={} name={}", player.id.take(8), player.name)

        val response = LoginVerifyResponse(
            token = token,
            uid = tokenUserId,
            displayName = displayName,
            guest = false
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/rooms")
    fun getActiveRooms(): ResponseEntity<Map<String, Any?>> {
        val rooms = gameService.getActiveRooms().map { room -> room.toResponseMap() }
        log.info("[API] GET /rooms -> {} rooms", rooms.size)
        return ResponseEntity.ok(mapOf("rooms" to rooms))
    }

    @PostMapping("/rooms/create")
    fun createRoom(
        @RequestHeader("Authorization", required = false) auth: String?,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        val token = extractToken(auth)
        log.info("[API] POST /rooms token={}", token?.take(8))
        val player = token?.let { gameService.getPlayerById(it) }
            ?: run {
                log.warn("[API] Create room: invalid player token={}", token?.take(8))
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid player"))
            }

        val name = body["name"] as? String ?: "Room"
        val timeControlSeconds = (body["timeControlSeconds"] as? Number)?.toInt() ?: 600
        val isPrivate = body["isPrivate"] as? Boolean ?: false

        val room = gameService.createRoom(player, name, timeControlSeconds, isPrivate)
        log.info("[API] Room created: id={} name={} by={}", room.id, name, player.name)
        return ResponseEntity.ok(mapOf("roomId" to room.id))
    }

    @PostMapping("/rooms/{roomId}/join")
    fun joinRoom(
        @PathVariable roomId: String,
        @RequestHeader("Authorization", required = false) auth: String?
    ): ResponseEntity<Map<String, Any?>> {
        val token = extractToken(auth)
        log.info("[API] POST /rooms/{}/join token={}", roomId, token?.take(8))
        val player = token?.let { gameService.getPlayerById(it) }
            ?: run {
                log.warn("[API] Join room: invalid player token={}", token?.take(8))
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid player"))
            }

        val room = gameService.joinRoom(roomId, player)
            ?: run {
                log.warn("[API] Join room FAILED: room={} player={}", roomId, player.name)
                return ResponseEntity.badRequest().body(mapOf("error" to "Cannot join room"))
            }

        log.info("[API] Joined room: id={} red={} black={} status={}", room.id, room.redPlayer?.name, room.blackPlayer?.name, room.status)
        return ResponseEntity.ok(room.toResponseMap())
    }

    private fun extractToken(auth: String?): String? {
        if (auth == null) return null
        return if (auth.startsWith("Bearer ")) auth.removePrefix("Bearer ") else auth
    }

    private fun GameRoom.toResponseMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "host" to mapOf(
            "id" to (redPlayer?.id ?: ""),
            "name" to (redPlayer?.name ?: ""),
            "xp" to (redPlayer?.xp ?: 1200)
        ),
        "guest" to blackPlayer?.let {
            mapOf("id" to it.id, "name" to it.name, "xp" to it.xp)
        },
        "status" to status.name.lowercase(),
        "timeControlSeconds" to timeControlSeconds,
        "isPrivate" to isPrivate
    )
}
