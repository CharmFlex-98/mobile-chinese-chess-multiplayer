package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.PlayerDto
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainMenuState(
    val playerName: String = "Guest",
    val playerRating: Int = 1200,
    val isLoggedIn: Boolean = false
)

class MainMenuViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MainMenuState())
    val state: StateFlow<MainMenuState> = _state.asStateFlow()

    init {
        observePlayer()
        autoLogin()
    }

    private fun observePlayer() {
        viewModelScope.launch {
            userRepository.currentPlayer.collect { player ->
                if (player != null) {
                    _state.update {
                        it.copy(
                            playerName = player.name,
                            playerRating = player.rating,
                            isLoggedIn = true
                        )
                    }
                }
            }
        }
    }

    private fun autoLogin() {
        if (!userRepository.isLoggedIn()) {
            viewModelScope.launch {
                val guestName = "Player_${(1000..9999).random()}"
                userRepository.loginAsGuest(guestName)
            }
        }
    }
}
