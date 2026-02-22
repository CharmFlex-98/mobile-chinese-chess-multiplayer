package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.RouteNavigator
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.route.AuthRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.storage.AuthLocalStorage
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.GameRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.route.GameRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.route.HomeRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

data class LeaderboardEntryUi(
    val rank: Int,
    val name: String,
    val xp: Int,
    val level: Int
)

data class MainMenuState(
    val playerName: String = "Guest",
    val playerRating: Int = 1200,
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = true,
    val xp: Int = 0,
    val level: Int = 1,
    val avatarUrl: String? = null,
    val isAdmin: Boolean = false,
    val leaderboard: List<LeaderboardEntryUi> = emptyList(),
    val showLeaderboard: Boolean = false,
    val isLeaderboardLoading: Boolean = false,
    val pendingLevelUp: Int? = null,
    val showAdminPanel: Boolean = false,
    val adminRooms: List<com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.BattleRoom> = emptyList(),
    val isAdminLoading: Boolean = false
)

@Factory
class MainMenuViewModel(
    private val sessionManager: SessionManager,
    private val routeNavigator: RouteNavigator,
    private val gameRepository: GameRepository,
    private val authLocalStorage: AuthLocalStorage
) : ViewModel() {
    private val _state = MutableStateFlow(MainMenuState())
    val state: StateFlow<MainMenuState> = _state.asStateFlow()

    init {
        observeAuthUser()
        observePendingLevelUp()
    }

    private fun observeAuthUser() {
        viewModelScope.launch {
            sessionManager.currentUserSession.collect { authUser ->
                if (authUser != null) {
                    _state.update {
                        it.copy(
                            playerName = authUser.name,
                            isLoggedIn = true,
                            isGuest = authUser.isGuest,
                            xp = authUser.xp,
                            level = authUser.level,
                            avatarUrl = authUser.avatarUrl,
                            isAdmin = authUser.isAdmin
                        )
                    }
                }
            }
        }
    }

    private fun observePendingLevelUp() {
        viewModelScope.launch {
            sessionManager.pendingLevelUp.collect { newLevel ->
                _state.update { it.copy(pendingLevelUp = newLevel) }
            }
        }
    }

    fun onLevelUpAcknowledged() {
        sessionManager.clearPendingLevelUp()
    }

    fun onNavigateToAISelection() {
        routeNavigator.navigateTo(GameRoute.AiSelect)
    }

    fun onShowLeaderboard() {
        _state.update { it.copy(showLeaderboard = true, isLeaderboardLoading = true, leaderboard = emptyList()) }
        viewModelScope.launch {
            val result = gameRepository.getLeaderboard()
            result.fold(
                onSuccess = { entries ->
                    _state.update {
                        it.copy(
                            leaderboard = entries.mapIndexed { index, e ->
                                LeaderboardEntryUi(rank = index + 1, name = e.name, xp = e.xp, level = e.level)
                            },
                            isLeaderboardLoading = false
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLeaderboardLoading = false) }
                }
            )
        }
    }

    fun onDismissLeaderboard() {
        _state.update { it.copy(showLeaderboard = false) }
    }

    fun onShowAdminPanel() {
        _state.update { it.copy(showAdminPanel = true, isAdminLoading = true, adminRooms = emptyList()) }
        viewModelScope.launch {
            gameRepository.getActiveRooms().fold(
                onSuccess = { rooms -> _state.update { it.copy(adminRooms = rooms, isAdminLoading = false) } },
                onFailure = { _state.update { it.copy(isAdminLoading = false) } }
            )
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            authLocalStorage.clear()
            routeNavigator.navigateAndPopUpTo(AuthRoute.Login, HomeRoute.ROOT)
        }
    }

    fun onDismissAdminPanel() {
        _state.update { it.copy(showAdminPanel = false) }
    }

    fun onSendAdminMessage(roomId: String, message: String) {
        viewModelScope.launch {
            gameRepository.sendAdminMessage(roomId, message)
        }
    }
}
