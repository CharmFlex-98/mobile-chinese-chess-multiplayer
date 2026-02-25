package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.AuthService
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.SessionNotFound
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.AuthRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.route.AuthRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.route.HomeRoute
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@Factory
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val routeNavigator: RouteNavigator,
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        restoreSession()
        observeSessionStatus()
    }

    fun restoreSession() {
        viewModelScope.launch {
            authService.restoreSession().onSuccess {
                _state.update {
                    it.copy(isRestoringSession = false)
                }
            }.onFailure { e ->
                val error = if (e is SessionNotFound) "" else "Login failed"
                _state.update {
                    it.copy(isRestoringSession = false, error = error)
                }
            }
        }
    }

    private fun observeSessionStatus() {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated && _state.value.isLoading) {
                    // OAuth redirect completed — register with game server
                    authService.handleUserAuthenticated().fold(
                        onSuccess = {
                            routeNavigator.navigateAndPopUpTo(HomeRoute.ROOT, AuthRoute.Login)
                        },
                        onFailure = { e ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Login failed"
                                )
                            }
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
                authRepository.signIn()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Google Sign-In failed"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun continueAsGuest() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val guestId = Uuid.random().toString()
            try {
                authRepository.signInAsGuest(guestId, "", "")
//                routeNavigator.navigateAndPopUpTo(HomeRoute.ROOT, AuthRoute.Login)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to continue as guest"
                    )
                }
            }
        }
    }
}

data class LoginState(
    val isRestoringSession: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)
