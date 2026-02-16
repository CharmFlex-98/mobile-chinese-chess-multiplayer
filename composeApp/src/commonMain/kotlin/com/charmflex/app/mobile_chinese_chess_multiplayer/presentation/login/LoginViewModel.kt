package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.UserRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    init {
        observeSessionStatus()
    }

    private fun observeSessionStatus() {
        viewModelScope.launch {
            userRepository.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated && _state.value.isLoading) {
                    // OAuth redirect completed — register with game server
                    userRepository.handleOAuthSession().fold(
                        onSuccess = { _authSuccess.value = true },
                        onFailure = { e ->
                            _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                        }
                    )
                }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Opens browser for Supabase OAuth. When the user completes sign-in,
                // the deep link callback triggers session status → Authenticated,
                // which is handled by observeSessionStatus() above.
                userRepository.signInWithGoogle()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Google Sign-In failed") }
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val guestName = "Guest_${(1000..9999).random()}"
            userRepository.loginAsGuest(guestName).fold(
                onSuccess = { _authSuccess.value = true },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to continue as guest") }
                }
            )
        }
    }
}
