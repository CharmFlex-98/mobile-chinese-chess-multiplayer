package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.storage

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.SharedPrefs
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.model.User
import com.russhwolf.settings.Settings
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Singleton

@Singleton
class AuthLocalStorage(
    private val settings: Settings = Settings(),
    private val sharedPrefs: SharedPrefs
) {
    fun saveSession(user: User, refreshToken: String = "") {
        user.email?.let { sharedPrefs.setString(KEY_USER_EMAIL, it) }
        sharedPrefs.setString(KEY_USER_ID, user.id)
        sharedPrefs.setString(KEY_DISPLAY_NAME, user.name)
        sharedPrefs.setBoolean(KEY_IS_GUEST, user.isGuest)
        sharedPrefs.setString(KEY_ACCESS_TOKEN, user.token)
        sharedPrefs.setString(KEY_REFRESH_TOKEN, refreshToken)
    }

    fun getSession(): User? {
        val email = sharedPrefs.getString(KEY_USER_EMAIL, "").ifEmpty { return null }
        val isGuest = sharedPrefs.getBoolean(KEY_IS_GUEST, true)
        val accessToken = sharedPrefs.getString(KEY_ACCESS_TOKEN, "").ifEmpty {  return null }
        val name = sharedPrefs.getString(KEY_DISPLAY_NAME, "").ifEmpty {  return null }
        val uid = sharedPrefs.getString(KEY_USER_ID, "").ifEmpty {  return null }

        return User(
            id = uid,
            token = accessToken,
            name = name,
            email = email,
            isGuest = isGuest
        )
    }

    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)?.takeIf { it.isNotEmpty() }

    fun clear() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_USER_EMAIL)
        settings.remove(KEY_DISPLAY_NAME)
        settings.remove(KEY_AUTH_TYPE)
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
    }

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_IS_GUEST = "auth_is_guest"
    }
}