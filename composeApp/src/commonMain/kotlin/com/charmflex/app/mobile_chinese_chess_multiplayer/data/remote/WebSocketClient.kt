package com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class WebSocketClient(
    private val wsUrl: String = "ws://10.0.2.2:8080/ws",
    private val httpClient: HttpClient = createWsClient()
) {
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val _serverMessages = MutableSharedFlow<WsServerMessage>(extraBufferCapacity = 64)
    val serverMessages: SharedFlow<WsServerMessage> = _serverMessages.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private var authToken: String? = null

    fun setAuthToken(token: String) {
        authToken = token
        println("[WS] Auth token set: ${token.take(8)}...")
    }

    suspend fun connect(scope: CoroutineScope) {
        connectionJob?.cancel()
        val url = buildWsUrl()
        println("[WS] Connecting to $url ...")
        connectionJob = scope.launch {
            try {
                _connectionState.value = false
                httpClient.webSocket(urlString = url) {
                    session = this
                    _connectionState.value = true
                    println("[WS] Connected successfully!")

                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    println("[WS] <<< RECV: $text")
                                    val msg = parseServerMessage(text)
                                    if (msg != null) {
                                        println("[WS] Parsed: ${msg::class.simpleName}")
                                        _serverMessages.emit(msg)
                                    } else {
                                        println("[WS] WARN: Could not parse message")
                                    }
                                }
                                else -> {}
                            }
                        }
                    } finally {
                        println("[WS] Connection closed (incoming loop ended)")
                        session = null
                        _connectionState.value = false
                    }
                }
            } catch (e: CancellationException) {
                println("[WS] Connection cancelled")
                throw e
            } catch (e: Exception) {
                println("[WS] Connection error: ${e::class.simpleName}: ${e.message}")
                session = null
                _connectionState.value = false
            }
        }
    }

    private fun buildWsUrl(): String {
        val token = authToken ?: return wsUrl
        return "$wsUrl?token=$token"
    }

    suspend fun send(message: WsClientMessage) {
        val envelope = WsEnvelope(
            type = message.type,
            payload = wsJson.encodeToJsonElement(serializeClientMessage(message))
        )
        val text = wsJson.encodeToString(envelope)
        println("[WS] >>> SEND: $text")
        val s = session
        if (s != null) {
            s.send(Frame.Text(text))
        } else {
            println("[WS] WARN: No active session, message dropped!")
        }
    }

    suspend fun sendMove(roomId: String, move: MoveDto) {
        send(WsClientMessage.MakeMove(roomId, move))
    }

    suspend fun joinQueue(timeControlSeconds: Int = 600) {
        println("[WS] Joining matchmaking queue (time=${timeControlSeconds}s)")
        send(WsClientMessage.QueueJoin(timeControlSeconds))
    }

    suspend fun leaveQueue() {
        println("[WS] Leaving matchmaking queue")
        send(WsClientMessage.QueueLeave)
    }

    suspend fun sendChat(roomId: String, message: String) {
        send(WsClientMessage.ChatSend(roomId, message))
    }

    suspend fun resign(roomId: String) {
        println("[WS] Resign in room $roomId")
        send(WsClientMessage.Resign(roomId))
    }

    suspend fun offerDraw(roomId: String) {
        send(WsClientMessage.DrawOffer(roomId))
    }

    suspend fun respondToDraw(roomId: String, accepted: Boolean) {
        send(WsClientMessage.DrawResponse(roomId, accepted))
    }

    suspend fun joinRoom(roomId: String) {
        println("[WS] Joining room WS: $roomId")
        send(WsClientMessage.RoomJoin(roomId))
    }

    suspend fun reportGameOver(roomId: String, result: String, reason: String) {
        println("[WS] Reporting game over: room=$roomId result=$result reason=$reason")
        send(WsClientMessage.GameOverReport(roomId, result, reason))
    }

    fun disconnect() {
        println("[WS] Disconnecting...")
        connectionJob?.cancel()
        connectionJob = null
        session = null
        _connectionState.value = false
    }

    private fun parseServerMessage(text: String): WsServerMessage? {
        return try {
            val envelope = wsJson.decodeFromString<WsEnvelope>(text)
            when (envelope.type) {
                "move_made" -> wsJson.decodeFromJsonElement<WsServerMessage.MoveMade>(envelope.payload)
                "game_started" -> wsJson.decodeFromJsonElement<WsServerMessage.GameStarted>(envelope.payload)
                "game_over" -> wsJson.decodeFromJsonElement<WsServerMessage.GameOver>(envelope.payload)
                "queue_update" -> wsJson.decodeFromJsonElement<WsServerMessage.QueueUpdate>(envelope.payload)
                "match_found" -> wsJson.decodeFromJsonElement<WsServerMessage.MatchFound>(envelope.payload)
                "chat_receive" -> wsJson.decodeFromJsonElement<WsServerMessage.ChatReceive>(envelope.payload)
                "timer_update" -> wsJson.decodeFromJsonElement<WsServerMessage.TimerUpdate>(envelope.payload)
                "opponent_joined" -> wsJson.decodeFromJsonElement<WsServerMessage.OpponentJoined>(envelope.payload)
                "opponent_disconnected" -> wsJson.decodeFromJsonElement<WsServerMessage.OpponentDisconnected>(envelope.payload)
                "opponent_reconnected" -> wsJson.decodeFromJsonElement<WsServerMessage.OpponentReconnected>(envelope.payload)
                "draw_offered" -> wsJson.decodeFromJsonElement<WsServerMessage.DrawOffered>(envelope.payload)
                "undo_requested" -> wsJson.decodeFromJsonElement<WsServerMessage.UndoRequested>(envelope.payload)
                "error" -> wsJson.decodeFromJsonElement<WsServerMessage.Error>(envelope.payload)
                else -> {
                    println("[WS] Unknown message type: ${envelope.type}")
                    null
                }
            }
        } catch (e: Exception) {
            println("[WS] Parse error: ${e::class.simpleName}: ${e.message}")
            println("[WS] Raw text was: $text")
            null
        }
    }

    private fun serializeClientMessage(message: WsClientMessage): JsonElement {
        return when (message) {
            is WsClientMessage.MakeMove -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.QueueJoin -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.QueueLeave -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.ChatSend -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.Resign -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.DrawOffer -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.DrawResponse -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.UndoRequest -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.UndoResponse -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.RoomJoin -> wsJson.encodeToJsonElement(message)
            is WsClientMessage.GameOverReport -> wsJson.encodeToJsonElement(message)
        }
    }

    companion object {
        fun createWsClient(): HttpClient {
            return HttpClient {
                install(WebSockets)
            }
        }
    }
}
