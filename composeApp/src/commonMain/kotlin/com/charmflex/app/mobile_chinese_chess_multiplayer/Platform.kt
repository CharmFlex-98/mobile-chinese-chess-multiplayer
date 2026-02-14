package com.charmflex.app.mobile_chinese_chess_multiplayer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform