package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.di

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.AuthService
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.AuthRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.ui.login.LoginViewModel

interface AuthInjector {
    fun getLoginViewModel(): LoginViewModel
    fun provideAuthService(): AuthService
}
