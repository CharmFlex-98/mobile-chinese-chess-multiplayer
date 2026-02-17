package com.charmflex.app.mobile_chinese_chess_multiplayer.core.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.data.local.AuthLocalStorage
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.local.GameLocalDataSource
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.ApiClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.SupabaseAuthClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.WebSocketClient
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.GameRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient

object AppModule {
    private var _instance: AppDependencies? = null

    val instance: AppDependencies
        get() = _instance ?: error("AppModule not initialized. Call AppModule.initialize() first.")

    fun initialize(
        serverBaseUrl: String = DEFAULT_HTTP_URL,
        wsUrl: String = DEFAULT_WS_URL
    ) {
        val supabaseClient = createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = DEEP_LINK_SCHEME
                host = DEEP_LINK_HOST
            }
        }

        _instance = AppDependencies(
            apiClient = ApiClient(baseUrl = serverBaseUrl),
            webSocketClient = WebSocketClient(wsUrl = wsUrl),
            localDataSource = GameLocalDataSource(),
            supabaseAuthClient = SupabaseAuthClient(supabaseClient),
            authLocalStorage = AuthLocalStorage()
        )
    }

    // Default: Android emulator loopback to host machine
    // Change to your LAN IP for real device testing (e.g., "http://192.168.1.100:8080")
    const val DEFAULT_HTTP_URL = "http://192.168.1.15:8080"
    const val DEFAULT_WS_URL = "ws://192.168.1.15:8080/ws"

    // TODO: Replace with your Supabase project values
    const val SUPABASE_URL = "https://rkfreguzvmbbybbbyivc.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_wz_KRKkDK3YYB_tM5jhFOQ_3zwdElIa"

    // Deep link scheme for OAuth callback
    // Redirect URL: com.charmflex.xiangqi://auth-callback
    // Configure this same URL in Supabase Dashboard → Auth → URL Configuration → Redirect URLs
    const val DEEP_LINK_SCHEME = "com.charmflex.xiangqi"
    const val DEEP_LINK_HOST = "auth-callback"
}

class AppDependencies(
    val apiClient: ApiClient,
    val webSocketClient: WebSocketClient,
    val localDataSource: GameLocalDataSource,
    val supabaseAuthClient: SupabaseAuthClient,
    val authLocalStorage: AuthLocalStorage
) {
    val userRepository: UserRepository by lazy {
        UserRepository(apiClient, webSocketClient, localDataSource, supabaseAuthClient, authLocalStorage)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(apiClient, webSocketClient)
    }
}
