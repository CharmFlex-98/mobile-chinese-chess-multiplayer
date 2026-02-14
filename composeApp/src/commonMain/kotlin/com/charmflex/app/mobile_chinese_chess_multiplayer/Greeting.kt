package com.charmflex.app.mobile_chinese_chess_multiplayer

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}