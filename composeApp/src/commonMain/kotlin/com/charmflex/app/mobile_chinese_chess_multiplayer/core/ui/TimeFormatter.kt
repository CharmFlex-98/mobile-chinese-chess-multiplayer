package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val min =  minutes.toString()
    val sec =  seconds.toString().padStart(2, '0')
    return "$min:$sec"
}
