package com.charmflex.app.mobile_chinese_chess_multiplayer.core.config

import com.charmflex.app.mobile_chinese_chess_multiplayer.AppConstant
import org.koin.core.annotation.Factory

class AndroidAppConfigProvider : AppConfigProvider {
    override fun baseUrl(): String {
        return AppConstant.DEFAULT_HTTP_URL
    }

    override fun wsUrl(): String {
        return AppConstant.DEFAULT_WS_URL
    }

}