package com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage.SharedPrefsFactory
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.annotation.Singleton

private const val FEM_SHARED_PREFS_FILE_PATH = "MCC-SHARED-PREFERENCES-PATH"

internal class AndroidSharedPreferencesFactory(
    private val appContext: Context
) : SharedPrefsFactory {
    override fun create(): Settings {
        requireNotNull(appContext)

        return try {
            internalCreate()
        } catch (exception: Exception) {
            appContext.deleteSharedPreferences(FEM_SHARED_PREFS_FILE_PATH)
            internalCreate()
        }
    }

    private fun internalCreate(): Settings {
        val encryptedSharedPrefs = EncryptedSharedPreferences.create(
            appContext,
            FEM_SHARED_PREFS_FILE_PATH,
            MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return SharedPreferencesSettings(encryptedSharedPrefs)
    }
}