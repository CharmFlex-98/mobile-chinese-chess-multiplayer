package com.charmflex.app.mobile_chinese_chess_multiplayer.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Singleton

@Singleton
internal class SharedPrefsImpl(
    private val sharedPrefsFactory: SharedPrefsFactory
) : SharedPrefs {
    private val settings: Settings by lazy {
        sharedPrefsFactory.create()
    }

    override fun setInt(key: String, value: Int) {
        settings[key] = value
    }

    override fun getInt(key: String, default: Int): Int {
        return settings.getInt(key, default)
    }

    override fun setString(key: String, value: String) {
        settings[key] = value
    }

    override fun getString(key: String, default: String): String {
        return settings.getString(key, default)
    }

    override fun setFloat(key: String, value: Float) {
        settings[key] = value
    }

    override fun getFloat(key: String, default: Float): Float {
        return settings.getFloat(key, default)
    }

    override fun setBoolean(key: String, value: Boolean) {
        settings[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return settings.getBoolean(key, default)
    }

    override fun setStringSet(key: String, value: Set<String>) {
        settings[key] = value
    }

    override fun getStringSet(key: String): Set<String> {
        return setOf()
    }

    override fun clearAllData() {
        settings.clear()
    }

}

interface SharedPrefs {

    fun setInt(key: String, value: Int)
    fun getInt(key: String, default: Int): Int
    fun setString(key: String, value: String)

    fun getString(key: String, default: String): String

    fun setFloat(key: String, value: Float)

    fun getFloat(key: String, default: Float): Float

    fun setBoolean(key: String, value: Boolean)

    fun getBoolean(key: String, default: Boolean): Boolean

    fun setStringSet(key: String, value: Set<String>)
    fun getStringSet(key: String): Set<String>

    fun clearAllData()
}

internal interface SharedPrefsFactory {
    fun create(): Settings
}