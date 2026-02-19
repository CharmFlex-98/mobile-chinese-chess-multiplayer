package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.network

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.GlobalChatReceive
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.GlobalChatSend
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.RealTimeCommunicationChannel
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.WsServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.annotation.Singleton

@Singleton
class GlobalChatChannel(
    private val socketClient: WebSocketClient
) : RealTimeCommunicationChannel(socketClient) {
    private val wsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun subscribeChannel(): Flow<WsServerMessage> {
        return channelMessage.mapNotNull {
            when (it.type) {
                "global_chat_receive" -> wsJson.decodeFromJsonElement<GlobalChatReceive>(it.payload)
                else -> null
            }
        }
    }

    suspend fun sendGlobalChat(message: String) {
        println("[WS] Sending global chat: $message")
        socketClient.send(GlobalChatSend(message))
    }

    override fun scene(): ChannelScene = GlobalChatScene
}

private val GlobalChatScene = object : RealTimeCommunicationChannel.ChannelScene {
    override val name: String get() = "global"
}
