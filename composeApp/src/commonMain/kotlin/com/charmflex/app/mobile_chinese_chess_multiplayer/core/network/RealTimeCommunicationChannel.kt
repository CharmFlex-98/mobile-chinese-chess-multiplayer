package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

abstract class RealTimeCommunicationChannel(
    private val socketClient: WebSocketClient
) {
    protected val channelMessage = socketClient.serverMessages.filter { it.scene == scene().name }

    interface ChannelScene {
        val name: String
    }
    abstract fun scene(): ChannelScene
    suspend fun connectWebSocket() {
        socketClient.connect()
    }
    fun isConnected(): Flow<Boolean> {
        return socketClient.connectionState
    }
}