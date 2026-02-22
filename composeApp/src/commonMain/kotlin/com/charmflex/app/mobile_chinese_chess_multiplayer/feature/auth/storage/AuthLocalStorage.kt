package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.storage

import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.SharedPrefs
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.model.User
import com.russhwolf.settings.Settings
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Singleton

@Singleton
class AuthLocalStorage(
    private val sharedPrefs: SharedPrefs
) {
    fun saveSession(user: User, refreshToken: String = "") {
        user.email?.let { sharedPrefs.setString(KEY_USER_EMAIL, it) }
        sharedPrefs.setString(KEY_USER_ID, user.id)
        sharedPrefs.setString(KEY_DISPLAY_NAME, user.name)
        sharedPrefs.setBoolean(KEY_IS_GUEST, user.isGuest)
        sharedPrefs.setString(KEY_ACCESS_TOKEN, user.token)
        sharedPrefs.setString(KEY_REFRESH_TOKEN, refreshToken)
        sharedPrefs.setInt(KEY_PLAYER_XP, user.xp)
        sharedPrefs.setInt(KEY_PLAYER_LVL, user.level)
        user.avatarUrl?.let { sharedPrefs.setString(KEY_PROFILE_URL, it) }
        sharedPrefs.setBoolean(KEY_PROFILE_ADMIN, user.admin)
    }

    fun getSession(): User? {
        val email = sharedPrefs.getString(KEY_USER_EMAIL, "").ifEmpty { return null }
        val isGuest = sharedPrefs.getBoolean(KEY_IS_GUEST, true)
        val accessToken = sharedPrefs.getString(KEY_ACCESS_TOKEN, "").ifEmpty {  return null }
        val name = sharedPrefs.getString(KEY_DISPLAY_NAME, "").ifEmpty {  return null }
        val uid = sharedPrefs.getString(KEY_USER_ID, "").ifEmpty {  return null }
        val xp = sharedPrefs.getInt(KEY_PLAYER_XP, 0)
        val level = sharedPrefs.getInt(KEY_PLAYER_LVL, 1)
        val profileUrl = sharedPrefs.getString(KEY_PROFILE_URL, "").ifEmpty {  return null }
        val isAdmin = sharedPrefs.getBoolean(KEY_PROFILE_ADMIN, false)

        return User(
            id = uid,
            token = accessToken,
            name = name,
            email = email,
            isGuest = isGuest,
            xp = xp,
            level = level,
            avatarUrl = profileUrl,
            admin = isAdmin
        )
    }

    fun clear() {
        sharedPrefs.remove(KEY_USER_ID)
        sharedPrefs.remove(KEY_USER_EMAIL)
        sharedPrefs.remove(KEY_DISPLAY_NAME)
        sharedPrefs.remove(KEY_AUTH_TYPE)
        sharedPrefs.remove(KEY_ACCESS_TOKEN)
        sharedPrefs.remove(KEY_REFRESH_TOKEN)
        sharedPrefs.remove(KEY_PLAYER_XP)
        sharedPrefs.remove(KEY_PLAYER_LVL)
        sharedPrefs.remove(KEY_PROFILE_URL)
        sharedPrefs.remove(KEY_PROFILE_ADMIN)
    }

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_IS_GUEST = "auth_is_guest"
        private const val KEY_PLAYER_XP = "player_xp"
        private const val KEY_PLAYER_LVL = "player_lvl"
        private const val KEY_PROFILE_URL = "profile_url"
        private const val KEY_PROFILE_ADMIN = "profile_admin"
    }
}