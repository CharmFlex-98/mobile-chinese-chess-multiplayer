package com.charmflex.xiangqi.server.websocket

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionRegistry {
    private val log = LoggerFactory.getLogger(SessionRegistry::class.java)
    val sessions = ConcurrentHashMap<String, WebSocketSession>()
    val lastHeartbeatsMap = ConcurrentHashMap<String, Long>()

    fun register(sessionId: String, session: WebSocketSession) {
        sessions[sessionId] = session
    }

    fun unregister(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun sendToSession(sessionId: String, message: String) {
        sessions[sessionId]?.let { s ->
            if (s.isOpen) {
                s.sendMessage(TextMessage(message))
            } else {
                log.warn("[REG] Session {} is closed, message dropped", sessionId)
            }
        } ?: run {
            log.warn("[REG] Session {} not found, message dropped", sessionId)
        }
    }

    fun broadcastToAll(message: String) {
        sessions.keys.forEach { sendToSession(it, message) }
    }

    fun getAllSessionIds(): Set<String> = sessions.keys
}
