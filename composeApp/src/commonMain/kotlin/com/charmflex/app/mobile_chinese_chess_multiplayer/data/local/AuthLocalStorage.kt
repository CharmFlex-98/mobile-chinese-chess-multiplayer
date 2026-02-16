package com.charmflex.app.mobile_chinese_chess_multiplayer.data.local

import com.charmflex.app.mobile_chinese_chess_multiplayer.domain.model.AuthType
import com.charmflex.app.mobile_chinese_chess_multiplayer.domain.model.AuthUser
import com.russhwolf.settings.Settings

class AuthLocalStorage(
    private val settings: Settings = Settings()
) {
    fun saveSession(user: AuthUser, refreshToken: String = "") {
        settings.putString(KEY_USER_ID, user.id)
        settings.putString(KEY_DISPLAY_NAME, user.displayName)
        settings.putString(KEY_EMAIL, user.email ?: "")
        settings.putString(KEY_AUTH_TYPE, user.authType.name)
        settings.putString(KEY_ACCESS_TOKEN, user.accessToken)
        settings.putString(KEY_REFRESH_TOKEN, refreshToken)
    }

    fun getSession(): AuthUser? {
        val id = settings.getStringOrNull(KEY_USER_ID) ?: return null
        val authTypeName = settings.getStringOrNull(KEY_AUTH_TYPE) ?: return null
        val authType = try { AuthType.valueOf(authTypeName) } catch (_: Exception) { return null }
        val accessToken = settings.getStringOrNull(KEY_ACCESS_TOKEN) ?: return null

        return AuthUser(
            id = id,
            displayName = settings.getString(KEY_DISPLAY_NAME, "Guest"),
            email = settings.getStringOrNull(KEY_EMAIL)?.takeIf { it.isNotEmpty() },
            authType = authType,
            accessToken = accessToken
        )
    }

    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)?.takeIf { it.isNotEmpty() }

    fun clear() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_DISPLAY_NAME)
        settings.remove(KEY_EMAIL)
        settings.remove(KEY_AUTH_TYPE)
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
    }

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
    }
}
