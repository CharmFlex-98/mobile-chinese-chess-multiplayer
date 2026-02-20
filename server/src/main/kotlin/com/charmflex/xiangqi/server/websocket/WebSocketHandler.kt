package com.charmflex.xiangqi.server.websocket

import com.charmflex.xiangqi.server.service.GameOrchestrator
import com.charmflex.xiangqi.server.service.JwtValidator
import com.charmflex.xiangqi.server.service.PlayerPersistenceService
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Component
class WebSocketHandler(
    private val orchestrator: GameOrchestrator,
    private val sessionRegistry: SessionRegistry,
    private val jwtValidator: JwtValidator,
    private val persistenceService: PlayerPersistenceService
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(WebSocketHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("[WS] Connection established: sessionId={} uri={}", session.id, session.uri)
        sessionRegistry.register(session.id, session)
        val params = session.uri?.query?.split("&")?.mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }?.toMap() ?: emptyMap()

        val token = params["token"]
        val name = params["name"]
        log.info("[WS] Connection params: token={} name={}", token?.take(8)?.plus("..."), name)

        when {
            token != null -> {
                if (jwtValidator.isJwtToken(token)) {
                    val jwtResult = jwtValidator.validateAndGetUserId(token) ?: run {
                        log.warn("[WS] JWT validation returned null for session {}", session.id)
                        sessionRegistry.sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                            WsType.ERROR,
                            ErrorPayload(code = "invalid_token", message = "Token validation failed")
                        ))
                        sessionRegistry.unregister(session.id)
                        return
                    }

                    val player = persistenceService.findById(jwtResult.userId)?.toPlayer()
                    if (player == null) {
                        sessionRegistry.sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                            WsType.ERROR,
                            ErrorPayload(code = "invalid_state", message = "No player found")
                        ))
                        return
                    }
                    orchestrator.registerPlayerSession(session.id, player)
                } else {
                    sessionRegistry.sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                        WsType.ERROR,
                        ErrorPayload(code = "invalid_token", message = "Invalid token")
                    ))
                }
            }

            else -> {
                sessionRegistry.sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                    WsType.ERROR,
                    ErrorPayload(code = "invalid_token", message = "No token provided")
                ))
            }
        }
        log.info("[WS] Total active sessions: {}", sessionRegistry.getAllSessionIds().size)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info("[WS] Connection closed: sessionId={} status={}", session.id, status)
        sessionRegistry.unregister(session.id)
        orchestrator.onConnectionClosed(session.id)
        log.info("[WS] Remaining sessions: {}", sessionRegistry.getAllSessionIds().size)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.info("[WS] <<< RECV from session {}: {}", session.id, message.payload)
        try {
            val envelope = json.decodeFromString<JsonObject>(message.payload)
            val scene = envelope["scene"]?.jsonPrimitive?.content ?: WsScene.GAME
            val type = envelope["type"]?.jsonPrimitive?.content ?: return
            val payload = envelope["payload"]?.jsonObject ?: return
            log.info("[WS] Message scene={} type={} from session={}", scene, type, session.id)

            orchestrator.handleMessage(session.id, scene, type, payload)
        } catch (e: Exception) {
            log.error("[WS] Error handling message from session {}: {}", session.id, e.message, e)
            sessionRegistry.sendToSession(session.id, WsMessageBuilder.buildGameMessage(
                WsType.ERROR,
                ErrorPayload(code = "parse_error", message = e.message ?: "Invalid message")
            ))
        }
    }
}
