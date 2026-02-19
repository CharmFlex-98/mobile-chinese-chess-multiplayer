package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.config.AppConfigProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement
import org.koin.core.annotation.Factory

@Factory
class WebSocketClient(
    private val appConfigProvider: AppConfigProvider,
    private val sessionManager: SessionManager
) {
    private val wsUrl: String = appConfigProvider.wsUrl()
    private val httpClient: HttpClient = createWsClient()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val _serverMessages = MutableSharedFlow<WsEnvelope>(extraBufferCapacity = 64)
    val serverMessages: SharedFlow<WsEnvelope> = _serverMessages.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()


    suspend fun connect() {
        coroutineScope {
            connectionJob?.cancel()
            val url = buildWsUrl()
            println("[WS] Connecting to $url ...")
            connectionJob = launch {
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
                    session = null
                    _connectionState.value = false
                    throw e
                } catch (e: Exception) {
                    println("[WS] Connection error: ${e::class.simpleName}: ${e.message}")
                    session = null
                    _connectionState.value = false
                }
            }
        }
    }

    private fun buildWsUrl(): String {
        val token = sessionManager.currentUserSession.value?.token ?: ""
        return "$wsUrl?token=$token"
    }

    suspend fun send(message: WsClientMessage) {
        val envelope = WsEnvelope(
            scene = message.scene,
            type = message.type,
            payload = wsJson.encodeToJsonElement(message)
        )
        val jsonElement = wsJson.encodeToJsonElement(envelope)
        val text = wsJson.encodeToString(jsonElement)
        println("[WS] >>> SEND: $text")
        val s = session
        if (s != null) {
            s.send(Frame.Text(text))
        } else {
            println("[WS] WARN: No active session, message dropped!")
        }
    }

    fun disconnect() {
        println("[WS] Disconnecting...")
        connectionJob?.cancel()
        connectionJob = null
        session = null
        _connectionState.value = false
    }

    private fun parseServerMessage(text: String): WsEnvelope? {
        return try {
            wsJson.decodeFromString<WsEnvelope>(text)
        } catch (e: Exception) {
            println("[WS] Parse error: ${e::class.simpleName}: ${e.message}")
            println("[WS] Raw text was: $text")
            null
        }
    }

    companion object {
        fun createWsClient(): HttpClient {
            return HttpClient {
                install(WebSockets.Plugin)
            }
        }
    }
}