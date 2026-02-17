package com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(
    private val baseUrl: String = "http://10.0.2.2:8080",
    private val httpClient: HttpClient = createHttpClient()
) {
    private var authToken: String? = null

    fun setAuthToken(token: String) {
        authToken = token
        println("[API] Auth token set: ${token.take(8)}...")
    }

    private fun HttpRequestBuilder.applyAuth() {
        authToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun login(username: String, password: String): AuthResponse {
        return httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.body()
    }

    suspend fun register(username: String, password: String): AuthResponse {
        return httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(username, password))
        }.body()
    }

    suspend fun loginWithSupabase(supabaseToken: String, displayName: String): AuthResponse {
        println("[API] POST $baseUrl/api/auth/supabase  name=$displayName")
        val response = httpClient.post("$baseUrl/api/auth/supabase") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $supabaseToken")
            setBody(mapOf("displayName" to displayName))
        }
        val bodyText = response.bodyAsText()
        println("[API] Supabase login response (${response.status}): $bodyText")
        return Json.decodeFromString(bodyText)
    }

    suspend fun loginAsGuest(name: String): AuthResponse {
        println("[API] POST $baseUrl/api/auth/guest  name=$name")
        val response = httpClient.post("$baseUrl/api/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(name, ""))
        }
        val bodyText = response.bodyAsText()
        println("[API] Guest login response (${response.status}): $bodyText")
        return response.body()
    }

    suspend fun getActiveRooms(): List<RoomDto> {
        println("[API] GET $baseUrl/api/rooms")
        val response = httpClient.get("$baseUrl/api/rooms") {
            applyAuth()
        }
        val bodyText = response.bodyAsText()
        println("[API] Active rooms response (${response.status}): $bodyText")
        return response.body<RoomListResponse>().rooms
    }

    suspend fun createRoom(request: CreateRoomRequest): CreateRoomResponse {
        println("[API] POST $baseUrl/api/rooms  name=${request.name} time=${request.timeControlSeconds}")
        val response = httpClient.post("$baseUrl/api/rooms") {
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val bodyText = response.bodyAsText()
        println("[API] Create room response (${response.status}): $bodyText")
        return response.body()
    }

    suspend fun joinRoom(roomId: String): RoomDto {
        println("[API] POST $baseUrl/api/rooms/$roomId/join")
        val response = httpClient.post("$baseUrl/api/rooms/$roomId/join") {
            applyAuth()
        }
        val bodyText = response.bodyAsText()
        println("[API] Join room response (${response.status}): $bodyText")
        return response.body()
    }

    suspend fun getPlayerProfile(playerId: String): PlayerDto {
        return httpClient.get("$baseUrl/api/players/$playerId") {
            applyAuth()
        }.body()
    }

    fun close() {
        httpClient.close()
    }

    companion object {
        fun createHttpClient(): HttpClient {
            return HttpClient {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("==========")
                            println("HTTP::$message")
                            println("==========")
                        }
                    }
                    level = LogLevel.ALL
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 15000
                    socketTimeoutMillis = 15000
                }


                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    })
                }
            }
        }
    }
}
