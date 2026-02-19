package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.repository.AuthRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.route.GameRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

data class MainMenuState(
    val playerName: String = "Guest",
    val playerRating: Int = 1200,
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = true
)

@Factory
class MainMenuViewModel(
    private val sessionManager: SessionManager,
    private val routeNavigator: RouteNavigator
) : ViewModel() {
    private val _state = MutableStateFlow(MainMenuState())
    val state: StateFlow<MainMenuState> = _state.asStateFlow()

    init {
        observeAuthUser()
    }

    private fun observeAuthUser() {
        viewModelScope.launch {
            sessionManager.currentUserSession.collect { authUser ->
                if (authUser != null) {
                    _state.update {
                        it.copy(
                            playerName = authUser.name,
                            isLoggedIn = true,
                            isGuest = authUser.isGuest
                        )
                    }
                }
            }
        }
    }

    fun onNavigateToAISelection() {
        routeNavigator.navigateTo(GameRoute.AiSelect)
    }
}
