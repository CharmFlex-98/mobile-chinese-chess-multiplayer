package com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: String,
    val name: String,
    val rating: Int = 1200
)

@Serializable
data class RoomDto(
    val id: String,
    val name: String,
    val host: PlayerDto,
    val guest: PlayerDto? = null,
    val status: String = "waiting",
    val timeControlSeconds: Int = 600,
    val isPrivate: Boolean = false
)

@Serializable
data class MoveDto(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int
)

@Serializable
data class CreateRoomRequest(
    val name: String,
    val timeControlSeconds: Int = 600,
    val isPrivate: Boolean = false
)

@Serializable
data class CreateRoomResponse(
    val roomId: String
)

@Serializable
data class JoinRoomRequest(
    val roomId: String
)

@Serializable
data class MatchmakingRequest(
    val timeControlSeconds: Int = 600
)

@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val player: PlayerDto
)

@Serializable
data class RoomListResponse(
    val rooms: List<RoomDto>
)
