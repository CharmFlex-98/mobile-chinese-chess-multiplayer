package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class ToastManager {
    private val _state: MutableStateFlow<ToastState?> = MutableStateFlow(null)
    val state = _state.asStateFlow()

    fun postMessage(message: String) {
        post(message, toastType = ToastType.NEUTRAL)
    }

    fun postSuccess(message: String) {
        post(message, ToastType.SUCCESS)
    }

    fun postError(message: String?) {
        val msg = message ?: "Unknown error"
        post(msg, ToastType.ERROR)
    }

    private fun post(message: String, toastType: ToastType) {
        _state.value = ToastState(message, toastType)
    }

    fun reset() {
        _state.value = null
    }
}

data class ToastState(
    val message: String,
    val toastType: ToastType
)

enum class ToastType {
    SUCCESS, ERROR, NEUTRAL
}