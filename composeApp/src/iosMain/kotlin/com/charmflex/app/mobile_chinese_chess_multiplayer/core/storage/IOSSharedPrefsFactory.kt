package com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSBundle

internal class IOSSharedPreferencesFactory : SharedPrefsFactory {

    @OptIn(ExperimentalSettingsImplementation::class)
    override fun create(): Settings {
        return KeychainSettings("${NSBundle.mainBundle.bundleIdentifier}.AUTH")
    }
}