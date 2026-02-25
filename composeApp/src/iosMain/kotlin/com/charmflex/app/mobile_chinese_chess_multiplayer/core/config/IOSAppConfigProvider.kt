package com.charmflex.app.mobile_chinese_chess_multiplayer.core.config

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.constant.AuthConstant
import org.koin.core.annotation.Factory

class IOSAppConfigProvider : AppConfigProvider {
    override fun baseUrl(): String {
        return AuthConstant.DEFAULT_HTTP_URL
    }

    override fun wsUrl(): String {
        return AuthConstant.DEFAULT_WS_URL
    }
}