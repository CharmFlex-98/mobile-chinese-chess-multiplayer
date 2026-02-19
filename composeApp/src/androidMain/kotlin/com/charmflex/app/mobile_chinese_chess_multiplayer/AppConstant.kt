package com.charmflex.app.mobile_chinese_chess_multiplayer

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.constant.AuthConstant

object AppConstant {
    // Default: Android emulator loopback to host machine
// Change to your LAN IP for real device testing (e.g., "http://192.168.1.100:8080")
    const val DEFAULT_HTTP_URL = AuthConstant.DEFAULT_HTTP_URL
    const val DEFAULT_WS_URL = AuthConstant.DEFAULT_WS_URL

    // TODO: Replace with your Supabase project values
    const val SUPABASE_URL = AuthConstant.SUPABASE_URL
    const val SUPABASE_ANON_KEY = AuthConstant.SUPABASE_ANON_KEY

    // Deep link scheme for OAuth callback
// Redirect URL: com.charmflex.xiangqi://auth-callback
// Configure this same URL in Supabase Dashboard → Auth → URL Configuration → Redirect URLs
    const val DEEP_LINK_SCHEME = AuthConstant.DEEP_LINK_SCHEME
    const val DEEP_LINK_HOST = AuthConstant.DEEP_LINK_HOST
}