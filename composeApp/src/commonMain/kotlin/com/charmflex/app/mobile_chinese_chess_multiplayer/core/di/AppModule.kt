package com.charmflex.app.mobile_chinese_chess_multiplayer.core.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.local.GameLocalDataSource
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.ApiClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.GameRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.UserRepository

object AppModule {
    private var _instance: AppDependencies? = null

    val instance: AppDependencies
        get() = _instance ?: createDefault().also { _instance = it }

    /**
     * Initialize with custom server URLs.
     *
     * For Android Emulator: use defaults (10.0.2.2:8080)
     * For real devices on same WiFi: use your computer's LAN IP, e.g.:
     *   AppModule.initialize("http://192.168.1.100:8080", "ws://192.168.1.100:8080/ws")
     */
    fun initialize(
        serverBaseUrl: String = DEFAULT_HTTP_URL,
        wsUrl: String = DEFAULT_WS_URL
    ) {
        _instance = AppDependencies(
            apiClient = ApiClient(baseUrl = serverBaseUrl),
            webSocketClient = WebSocketClient(wsUrl = wsUrl),
            localDataSource = GameLocalDataSource()
        )
    }

    private fun createDefault(): AppDependencies {
        return AppDependencies(
            apiClient = ApiClient(baseUrl = DEFAULT_HTTP_URL),
            webSocketClient = WebSocketClient(wsUrl = DEFAULT_WS_URL),
            localDataSource = GameLocalDataSource()
        )
    }

    // Default: Android emulator loopback to host machine
    // Change to your LAN IP for real device testing (e.g., "http://192.168.1.100:8080")
    const val DEFAULT_HTTP_URL = "http://192.168.0.47:8080"
    const val DEFAULT_WS_URL = "ws://192.168.0.47:8080/ws"
}

class AppDependencies(
    val apiClient: ApiClient,
    val webSocketClient: WebSocketClient,
    val localDataSource: GameLocalDataSource
) {
    val userRepository: UserRepository by lazy {
        UserRepository(apiClient, webSocketClient, localDataSource)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(apiClient, webSocketClient)
    }
}
