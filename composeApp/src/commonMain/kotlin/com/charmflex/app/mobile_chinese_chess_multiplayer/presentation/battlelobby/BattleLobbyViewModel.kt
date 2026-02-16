package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.battlelobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.RoomDto
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote.dto.WsServerMessage
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.GameRepository
import com.charmflex.app.mobile_chinese_chess_multiplayer.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BattleLobbyState(
    val matchmakingStatus: MatchmakingStatus = MatchmakingStatus.IDLE,
    val queuePosition: Int = 0,
    val estimatedWaitSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val activeRooms: List<RoomDto> = emptyList(),
    val isLoadingRooms: Boolean = false,
    val matchFoundRoomId: String? = null,
    val matchFoundOpponentName: String? = null,
    val matchFoundPlayerColor: String? = null,
    val matchFoundIsCreator: Boolean = false,
    val isConnected: Boolean = false,
    val isGuest: Boolean = false,
    val error: String? = null
)

enum class MatchmakingStatus {
    IDLE,
    SEARCHING,
    MATCH_FOUND
}

class BattleLobbyViewModel(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BattleLobbyState())
    val state: StateFlow<BattleLobbyState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var matchmakingObserverJob: Job? = null

    init {
        observeConnection()
        observeAuthState()
        loadActiveRooms()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            gameRepository.isConnected.collect { connected ->
                println("[LOBBY] WS connection state: $connected")
                _state.update { it.copy(isConnected = connected) }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            userRepository.authUser.collect { authUser ->
                _state.update { it.copy(isGuest = authUser?.isGuest == true) }
            }
        }
    }

    fun connectAndSetup() {
        println("[LOBBY] connectAndSetup called, isLoggedIn=${userRepository.isLoggedIn()}")
        if (!userRepository.isLoggedIn()) {
            println("[LOBBY] Not logged in, cannot connect")
            _state.update { it.copy(error = "Not logged in") }
            return
        }
        viewModelScope.launch {
            println("[LOBBY] Connecting WebSocket...")
            gameRepository.connectWebSocket(viewModelScope)
        }
    }

    fun loadActiveRooms() {
        viewModelScope.launch {
            println("[LOBBY] Loading active rooms...")
            _state.update { it.copy(isLoadingRooms = true) }
            gameRepository.getActiveRooms()
                .onSuccess { rooms ->
                    println("[LOBBY] Got ${rooms.size} active rooms")
                    _state.update { it.copy(activeRooms = rooms, isLoadingRooms = false) }
                }
                .onFailure { e ->
                    println("[LOBBY] Failed to load rooms: ${e.message}")
                    _state.update { it.copy(isLoadingRooms = false, error = e.message) }
                }
        }
    }

    fun startMatchmaking(timeControlSeconds: Int = 600) {
        if (_state.value.isGuest) {
            _state.update { it.copy(error = "Sign in to play multiplayer") }
            return
        }
        if (_state.value.matchmakingStatus == MatchmakingStatus.SEARCHING) return
        println("[LOBBY] Starting matchmaking (time=${timeControlSeconds}s)")

        _state.update {
            it.copy(
                matchmakingStatus = MatchmakingStatus.SEARCHING,
                elapsedSeconds = 0,
                error = null
            )
        }

        // Start elapsed timer
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }

        // Observe matchmaking events
        matchmakingObserverJob?.cancel()
        matchmakingObserverJob = viewModelScope.launch {
            gameRepository.observeMatchmaking().collect { msg ->
                when (msg) {
                    is WsServerMessage.QueueUpdate -> {
                        println("[LOBBY] Queue update: position=${msg.position} est=${msg.estimatedWaitSeconds}s")
                        _state.update {
                            it.copy(
                                queuePosition = msg.position,
                                estimatedWaitSeconds = msg.estimatedWaitSeconds
                            )
                        }
                    }
                    is WsServerMessage.MatchFound -> {
                        println("[LOBBY] MATCH FOUND! room=${msg.roomId} opponent=${msg.opponent.name} color=${msg.playerColor}")
                        timerJob?.cancel()
                        _state.update {
                            it.copy(
                                matchmakingStatus = MatchmakingStatus.MATCH_FOUND,
                                matchFoundRoomId = msg.roomId,
                                matchFoundOpponentName = msg.opponent.name,
                                matchFoundPlayerColor = msg.playerColor
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            gameRepository.joinMatchmaking(timeControlSeconds)
        }
    }

    fun cancelMatchmaking() {
        println("[LOBBY] Cancelling matchmaking")
        timerJob?.cancel()
        matchmakingObserverJob?.cancel()
        _state.update {
            it.copy(
                matchmakingStatus = MatchmakingStatus.IDLE,
                elapsedSeconds = 0,
                queuePosition = 0
            )
        }
        viewModelScope.launch {
            gameRepository.leaveMatchmaking()
        }
    }

    fun joinRoom(roomId: String) {
        if (_state.value.isGuest) {
            _state.update { it.copy(error = "Sign in to join rooms") }
            return
        }
        println("[LOBBY] Joining room: $roomId")
        viewModelScope.launch {
            gameRepository.joinRoom(roomId)
                .onSuccess { room ->
                    println("[LOBBY] Joined room OK: ${room.id} host=${room.host.name}")
                    _state.update {
                        it.copy(
                            matchFoundRoomId = room.id,
                            matchFoundOpponentName = room.host.name,
                            matchFoundPlayerColor = "BLACK",
                            matchmakingStatus = MatchmakingStatus.MATCH_FOUND
                        )
                    }
                }
                .onFailure { e ->
                    println("[LOBBY] Join room FAILED: ${e.message}")
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    fun createRoom(name: String, timeControlSeconds: Int = 600, isPrivate: Boolean = false) {
        if (_state.value.isGuest) {
            _state.update { it.copy(error = "Sign in to create rooms") }
            return
        }
        println("[LOBBY] Creating room: $name")
        viewModelScope.launch {
            gameRepository.createRoom(name, timeControlSeconds, isPrivate)
                .onSuccess { roomId ->
                    println("[LOBBY] Room created: $roomId")
                    _state.update {
                        it.copy(
                            matchFoundRoomId = roomId,
                            matchFoundOpponentName = "Waiting...",
                            matchFoundPlayerColor = "RED",
                            matchFoundIsCreator = true,
                            matchmakingStatus = MatchmakingStatus.MATCH_FOUND
                        )
                    }
                }
                .onFailure { e ->
                    println("[LOBBY] Create room FAILED: ${e.message}")
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    fun clearMatchFound() {
        _state.update {
            it.copy(
                matchmakingStatus = MatchmakingStatus.IDLE,
                matchFoundRoomId = null,
                matchFoundOpponentName = null,
                matchFoundPlayerColor = null,
                matchFoundIsCreator = false
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        matchmakingObserverJob?.cancel()
    }
}
