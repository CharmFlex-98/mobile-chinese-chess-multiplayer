package com.charmflex.app.mobile_chinese_chess_multiplayer

import androidx.compose.ui.window.ComposeUIViewController
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppModule

fun MainViewController() = run {
    AppModule.initialize()
    ComposeUIViewController { App() }
}
