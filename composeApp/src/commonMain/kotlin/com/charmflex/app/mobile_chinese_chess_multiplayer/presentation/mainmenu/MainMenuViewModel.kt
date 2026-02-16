package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainMenuState(
    val playerName: String = "Guest",
    val playerRating: Int = 1200,
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = true
)

class MainMenuViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MainMenuState())
    val state: StateFlow<MainMenuState> = _state.asStateFlow()

    init {
        observeAuthUser()
        observePlayer()
    }

    private fun observeAuthUser() {
        viewModelScope.launch {
            userRepository.authUser.collect { authUser ->
                if (authUser != null) {
                    _state.update {
                        it.copy(
                            playerName = authUser.displayName,
                            isLoggedIn = true,
                            isGuest = authUser.isGuest
                        )
                    }
                }
            }
        }
    }

    private fun observePlayer() {
        viewModelScope.launch {
            userRepository.currentPlayer.collect { player ->
                if (player != null) {
                    _state.update {
                        it.copy(playerRating = player.rating)
                    }
                }
            }
        }
    }
}
