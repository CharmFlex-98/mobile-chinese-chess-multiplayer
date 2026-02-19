package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager

interface SessionInjector {
    fun sessionManager(): SessionManager
}