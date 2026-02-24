package com.charmflex.xiangqi.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

@Service
class DiscordService(
    @Value("\${discord.webhook.url}") private val webhookUrl: String,
    @Value("\${discord.webhook.textUrl}") private val webhookUrlText: String,
    @Value("\${discord.bot.token}") private val botToken: String
) {
    private val log = LoggerFactory.getLogger(DiscordService::class.java)
    private val httpClient = HttpClient.newHttpClient()
    private val objectMapper = ObjectMapper()
    // roomId -> Discord thread channel ID
    private val roomThreads = ConcurrentHashMap<String, String>()

    private data class QueuedRequest(
        val request: HttpRequest,
        val onSuccess: ((HttpResponse<String>) -> Unit)? = null
    )

    private val queue = LinkedBlockingQueue<QueuedRequest>()
    private val dispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "discord-queue").also { it.isDaemon = true }
    }
    // messages buffered while thread creation is in-flight
    private val pendingByRoom = ConcurrentHashMap<String, MutableList<String>>()
    // guards the check-then-add / store-then-flush pair
    private val threadRegistryLock = Any()

    @PostConstruct
    fun startDispatcher() {
        dispatcher.submit { drainLoop() }
    }

    @PreDestroy
    fun shutdown() {
        dispatcher.shutdownNow()
    }

    private fun drainLoop() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                dispatchWithRetry(queue.take())
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    private fun dispatchWithRetry(task: QueuedRequest) {
        while (true) {
            try {
                val response = httpClient.send(task.request, HttpResponse.BodyHandlers.ofString())
                when (response.statusCode()) {
                    in 200..299 -> { task.onSuccess?.invoke(response); return }
                    429 -> {
                        val retryAfterMs = parseRetryAfter(response.body())
                        log.warn("[DISCORD] Rate limited — retrying after {}ms", retryAfterMs)
                        Thread.sleep(retryAfterMs)
                    }
                    else -> {
                        log.warn("[DISCORD] Request failed: status={}, body={}", response.statusCode(), response.body())
                        return
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                log.warn("[DISCORD] Request error: {}", e.message)
                return
            }
        }
    }

    private fun parseRetryAfter(body: String): Long {
        return try {
            (objectMapper.readTree(body)["retry_after"]?.asDouble() ?: 1.0)
                .times(1000).toLong().coerceAtLeast(500)
        } catch (_: Exception) { 1000L }
    }

    fun notifyRoomCreated(roomId: String, roomName: String, creatorId: String, creatorName: String) {
        createThread(roomId, "Room: $roomName", "🏠 Room created by **$creatorName ($creatorId)** — `$roomName` (`$roomId`) · waiting for opponent")
    }

    fun notifyDestroyRoomWhileWaiting(roomId: String, roomName: String, creatorName: String) {
        sendToRoom(roomId, "🏠 Room `$roomName` (`$roomId`) destroyed by creator **$creatorName** while waiting")
    }

    fun notifyCleanStaledRoom(roomId: String, roomName: String) {
        val msg = "🏠 Room `$roomName` (${roomId}) is clean up by system"
        enqueueWebhook(msg)
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
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://discord.com/api/v10/channels/$threadId"))
            .header("Authorization", "Bot $botToken")
            .DELETE().build()
        queue.offer(QueuedRequest(request) { response ->
            log.info("[DISCORD] Deleted thread: threadId={}, status={}", threadId, response.statusCode())
        })
    }

    /**
     * Creates a new Discord Forum thread for the room and posts the first message into it.
     * Uses `?wait=true` so the response contains the `channel_id` (thread ID) we need to
     * route subsequent room events to the same thread.
     *
     * Requires the webhook to target a Discord **Forum channel**.
     * Fully async — the onSuccess callback stores the threadId and flushes any buffered messages.
     */
    private fun createThread(roomId: String, threadName: String, content: String) {
        if (webhookUrl.isBlank()) return
        val body = objectMapper.writeValueAsString(mapOf("content" to content, "thread_name" to threadName))
        val request = buildPostRequest("$webhookUrl?wait=true", body)
        queue.offer(QueuedRequest(request) { response ->
            val threadId = objectMapper.readTree(response.body())["channel_id"]?.asText()
            if (!threadId.isNullOrBlank()) {
                synchronized(threadRegistryLock) {
                    roomThreads[roomId] = threadId
                    pendingByRoom.remove(roomId)?.forEach { msg ->
                        enqueueWebhook(msg, threadId)
                    }
                }
                log.info("[DISCORD] Thread created for room {}: threadId={}", roomId, threadId)
            } else {
                log.warn("[DISCORD] Thread creation for room {} missing channel_id — response: {}", roomId, response.body())
            }
        })
    }

    /**
     * Posts a message to the room's thread. Buffers the message if the thread is not yet ready
     * (i.e. createThread is still in-flight) and flushes once the thread ID is known.
     */
    private fun sendToRoom(roomId: String, content: String) {
        synchronized(threadRegistryLock) {
            val threadId = roomThreads[roomId]
            if (threadId != null) {
                enqueueWebhook(content, threadId)
            } else {
                pendingByRoom.computeIfAbsent(roomId) { mutableListOf() }.add(content)
                log.debug("[DISCORD] Buffered message for room {} — thread not yet created", roomId)
            }
        }
    }

    private fun enqueueWebhook(content: String, threadId: String? = null) {
        if (webhookUrl.isBlank()) return
        val body = objectMapper.writeValueAsString(mapOf("content" to content))
        val url = if (threadId != null) "$webhookUrl?thread_id=$threadId" else webhookUrlText
        queue.offer(QueuedRequest(buildPostRequest(url, body)))
    }

    private fun buildPostRequest(url: String, body: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
}
