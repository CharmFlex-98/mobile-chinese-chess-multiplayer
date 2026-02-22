package com.charmflex.xiangqi.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap

@Service
class DiscordService(
    @Value("\${discord.webhook.url}") private val webhookUrl: String,
    @Value("\${discord.bot.token}") private val botToken: String
) {
    private val log = LoggerFactory.getLogger(DiscordService::class.java)
    private val httpClient = HttpClient.newHttpClient()
    private val objectMapper = ObjectMapper()
    // roomId -> Discord thread channel ID
    private val roomThreads = ConcurrentHashMap<String, String>()

    fun notifyRoomCreated(roomId: String, roomName: String, creatorId: String, creatorName: String) {
        createThread(roomId, "Room: $roomName", "🏠 Room created by **$creatorName ($creatorId)** — `$roomName` (`$roomId`) · waiting for opponent")
    }

    fun notifyDestroyRoomWhileWaiting(roomId: String, roomName: String, creatorName: String) {
        sendToRoom(roomId, "🏠 Room `$roomName` (`$roomId`) destroyed by creator **$creatorName** while waiting")
    }

    fun notifyRoomJoined(roomId: String, roomName: String, joinerId: String, joiner: String) {
        sendToRoom(roomId, "⚔️ **${joiner} ($joinerId)** joined the room — `$roomName` (`$roomId`)")
    }

    fun notifyGameOver(roomId: String, result: String, reason: String) {
        val resultText = when (result) {
            "red_wins" -> "🔴 Red wins"
            "black_wins" -> "⚫ Black wins"
            "draw" -> "🤝 Draw"
            else -> result
        }
        sendToRoom(roomId, "🏁 Game over — $resultText (_${reason}_)")
    }

    fun notifyChat(senderName: String, message: String, roomId: String) {
        sendToRoom(roomId, "💬 **$senderName**: $message")
    }

    fun removeRoom(roomId: String) {
        val threadId = roomThreads.remove(roomId) ?: return
        archiveThread(threadId)
    }

    private fun archiveThread(threadId: String) {
        if (botToken.isBlank()) {
            log.warn("[DISCORD] Bot token not configured — skipping thread delete for threadId={}", threadId)
            return
        }
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/channels/$threadId"))
                .header("Authorization", "Bot $botToken")
                .DELETE()
                .build()
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.warn("[DISCORD] Failed to delete thread {}: {}", threadId, e.message)
        }
    }

    /**
     * Creates a new Discord Forum thread for the room and posts the first message into it.
     * Uses `?wait=true` so the response contains the `channel_id` (thread ID) we need to
     * route subsequent room events to the same thread.
     *
     * Requires the webhook to target a Discord **Forum channel**.
     */
    private fun createThread(roomId: String, threadName: String, content: String) {
        if (webhookUrl.isBlank()) return
        try {
            val body = objectMapper.writeValueAsString(
                mapOf("content" to content, "thread_name" to threadName)
            )
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$webhookUrl?wait=true"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            // Synchronous so we capture threadId before any follow-up events arrive
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val threadId = objectMapper.readTree(response.body())["channel_id"]?.asText()
            if (!threadId.isNullOrBlank()) {
                roomThreads[roomId] = threadId
            } else {
                log.warn("[DISCORD] Thread creation for room {} missing channel_id — response: {}", roomId, response.body())
            }
        } catch (e: Exception) {
            log.warn("[DISCORD] Failed to create thread for room {}: {}", roomId, e.message)
        }
    }

    /**
     * Posts a message to the room's thread. Falls back to the main channel if no thread
     * exists for the room (e.g. after a server restart or thread creation failure).
     */
    private fun sendToRoom(roomId: String, content: String) {
        val threadId = roomThreads[roomId]
        sendWebhook(content, threadId)
    }

    private fun sendWebhook(content: String, threadId: String? = null) {
        if (webhookUrl.isBlank()) return
        if (threadId == null) {
            log.warn("[DISCORD] Dropping webhook message — no threadId available (thread creation may have failed)")
            return
        }
        try {
            val body = objectMapper.writeValueAsString(mapOf("content" to content))
            val url = "$webhookUrl?thread_id=$threadId"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete { response, ex ->
                    if (ex != null) {
                        log.warn("[DISCORD] Webhook send failed: {}", ex.message)
                    } else if (response.statusCode() !in 200..299) {
                        log.warn("[DISCORD] Webhook returned non-2xx: status={}, body={}", response.statusCode(), response.body())
                    }
                }
        } catch (e: Exception) {
            log.warn("[DISCORD] Failed to send webhook: {}", e.message)
        }
    }
}
