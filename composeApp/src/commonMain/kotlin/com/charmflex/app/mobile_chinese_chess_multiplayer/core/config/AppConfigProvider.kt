package com.charmflex.app.mobile_chinese_chess_multiplayer.core.config

interface AppConfigProvider {
    fun baseUrl(): String
    fun wsUrl(): String
}