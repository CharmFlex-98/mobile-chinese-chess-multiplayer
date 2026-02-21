package com.charmflex.xiangqi.server.service

import com.charmflex.xiangqi.server.websocket.SessionRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.socket.PingMessage


@Service
class SessionService(
    private val sessionRegistry: SessionRegistry
) {

    @Scheduled(fixedRate = 5000)
    fun cleanupDeadSessions() {
        sessionRegistry.sessions.forEach { sessionId, session ->
            val currentTime = System.currentTimeMillis()
            val lastTime = sessionRegistry.lastHeartbeatsMap[sessionId] ?: return@forEach
            if (currentTime - lastTime > 10_000) {
                session.close()
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    fun sendPing() {
        sessionRegistry.sessions.values.forEach { session ->
            if (session.isOpen) {
                try {
                    session.sendMessage(PingMessage())
                } catch (e: Exception) {
                    session.close()
                }
            }
        }
    }
}