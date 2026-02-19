package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.constant

object AuthConstant {
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