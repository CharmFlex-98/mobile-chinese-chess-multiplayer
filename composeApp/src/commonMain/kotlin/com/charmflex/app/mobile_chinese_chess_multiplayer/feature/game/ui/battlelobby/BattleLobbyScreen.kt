package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.ui.battlelobby

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.BattleRoom
import kotlinx.coroutines.delay
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.SurfaceDark

@Composable
fun BattleLobbyScreen(
    viewModel: BattleLobbyViewModel,
    onNavigateToGame: (roomId: String, opponentName: String, playerColor: String, isCreator: Boolean) -> Unit = { _, _, _, _ -> },
    onNavigateToSpectate: (roomId: String, redPlayerName: String, blackPlayerName: String) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.state.collectAsState()

    // Navigate when match is found
    LaunchedEffect(state.matchFoundRoomId) {
        val roomId = state.matchFoundRoomId
        if (roomId != null && state.matchmakingStatus == MatchmakingStatus.MATCH_FOUND) {
            onNavigateToGame(
                roomId,
                state.matchFoundOpponentName ?: "Opponent",
                state.matchFoundPlayerColor ?: "RED",
                state.matchFoundIsCreator
            )
            viewModel.clearMatchFound()
        }
    }

    // Navigate when watch room is confirmed
    LaunchedEffect(state.watchRoomId) {
        val roomId = state.watchRoomId
        if (roomId != null) {
            onNavigateToSpectate(roomId, state.watchRedPlayerName, state.watchBlackPlayerName)
            viewModel.clearWatchRoom()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LobbyHeader(
            isConnected = state.isConnected,
            onRefresh = { viewModel.loadActiveRooms() }
        )

        // Guest restriction banner
        if (state.isGuest) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("!", fontSize = 16.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Sign in to play multiplayer games. Guests can only spectate.",
                    style = AppTypography.bodySmall,
                    color = Color(0xFFFF9800)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                MatchmakingRadarSection(
                    status = state.matchmakingStatus,
                    elapsedSeconds = state.elapsedSeconds,
                    onStartMatchmaking = { viewModel.startMatchmaking() },
                    onCancelMatchmaking = { viewModel.cancelMatchmaking() }
                )
            }
            item {
                QueueStabilitySection(
                    queuePosition = state.queuePosition,
                    estimatedWaitSeconds = state.estimatedWaitSeconds,
                    isSearching = state.matchmakingStatus == MatchmakingStatus.SEARCHING
                )
            }
            item {
                ActiveRoomsHeader(roomCount = state.activeRooms.size)
            }
            items(state.activeRooms) { room ->
                RoomCard(
                    room = room,
                    isGuest = state.isGuest,
                    onJoin = { viewModel.joinRoom(room.id) },
                    onWatch = { viewModel.showWatchConfirmation(room) }
                )
            }
            if (state.activeRooms.isEmpty() && !state.isLoadingRooms) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No active rooms",
                            style = AppTypography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            item {
                CreateRoomSection(
                    onCreateRoom = { viewModel.showCreateRoomDialog() },
                    onJoinById = { viewModel.showJoinByIdDialog() },
                    isGuest = state.isGuest
                )
            }
        }
    }

    // Watch confirmation dialog
    state.pendingWatchRoom?.let { room ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissWatchConfirmation() },
            title = { Text("Watch This Game?", color = Color.White) },
            text = {
                Column {
                    Text(
                        room.name,
                        style = AppTypography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${room.host?.name ?: "Host"} vs ${room.guest?.name ?: "Opponent"}",
                        style = AppTypography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⏱ ${room.timeControlSeconds / 60} min · In progress",
                        style = AppTypography.bodySmall,
                        color = GoldPrimary.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmWatchRoom() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                ) {
                    Text("👁  Watch", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissWatchConfirmation() }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Rejoin dialog
    state.rejoinInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.onRejoinDeclined() },
            title = { Text("Resume Game?", color = Color.White) },
            text = {
                Text(
                    "You have an unfinished game against ${info.opponentName}. Continue or forfeit?",
                    style = AppTypography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onRejoinAccepted() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onRejoinDeclined() }) {
                    Text("Forfeit", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Create Room dialog
    if (state.showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { viewModel.dismissCreateRoomDialog() },
            onConfirm = { name, password ->
                viewModel.dismissCreateRoomDialog()
                // Private rooms are always private; password adds extra security
                viewModel.createRoom(name, isPrivate = true, password = password)
            }
        )
    }

    // Join by Room ID dialog
    if (state.showJoinByIdDialog) {
        JoinByRoomIdDialog(
            onDismiss = { viewModel.dismissJoinByIdDialog() },
            onConfirm = { roomId, password ->
                viewModel.dismissJoinByIdDialog()
                viewModel.joinRoom(roomId, password)
            }
        )
    }

    // Error snackbar
    state.error?.let { error ->
        LaunchedEffect(error) {
            if (error.isNotBlank()) {
                viewModel.showErrorSnackBar(error)
            }
            viewModel.dismissError()
        }
    }
}

@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, password: String?) -> Unit
) {
    var roomName by remember { mutableStateOf("My Room") }
    var password by remember { mutableStateOf("") }
    var usePassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Private Room", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it; if (!it) password = "" },
                        colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Password protect", color = Color.White, fontSize = 14.sp)
                }
                if (usePassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pw = if (usePassword && password.isNotBlank()) password else null
                    onConfirm(roomName.ifBlank { "My Room" }, pw)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
            ) { Text("Create", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = SurfaceDark
    )
}

@Composable
private fun JoinByRoomIdDialog(
    onDismiss: () -> Unit,
    onConfirm: (roomId: String, password: String?) -> Unit
) {
    var roomId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join by Room ID", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it },
                    label = { Text("Room ID", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (if any)", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomId.isNotBlank()) {
                        onConfirm(roomId.trim(), password.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                enabled = roomId.isNotBlank()
            ) { Text("Join", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = SurfaceDark
    )
}

@Composable
private fun LobbyHeader(isConnected: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark.copy(alpha = 0.95f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚔", fontSize = 20.sp, color = GoldPrimary)
            Spacer(Modifier.width(12.dp))
            Text("BATTLE LOBBY", style = AppTypography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        CircleShape
                    )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) { Text("↻", fontSize = 18.sp, color = GoldPrimary) }
        }
    }
}

@Composable
private fun MatchmakingRadarSection(
    status: MatchmakingStatus,
    elapsedSeconds: Int,
    onStartMatchmaking: () -> Unit,
    onCancelMatchmaking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.05f), Color.Transparent)))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            if (status == MatchmakingStatus.SEARCHING) {
                val infiniteTransition = rememberInfiniteTransition()
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f, targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f, targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
                )
                Box(modifier = Modifier.size(160.dp).scale(pulseScale).border(1.dp, GoldPrimary.copy(alpha = pulseAlpha), CircleShape))
            }
            Box(modifier = Modifier.size(220.dp).border(1.dp, GoldPrimary.copy(alpha = 0.1f), CircleShape))
            Box(
                modifier = Modifier.size(96.dp).background(
                    if (status == MatchmakingStatus.SEARCHING) GoldPrimary else GoldPrimary.copy(alpha = 0.5f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (status == MatchmakingStatus.SEARCHING) "🔍" else "⚔", fontSize = 36.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            when (status) {
                MatchmakingStatus.IDLE -> "Ready to Battle"
                MatchmakingStatus.SEARCHING -> "Searching for Opponent..."
                MatchmakingStatus.MATCH_FOUND -> "Match Found!"
            },
            style = AppTypography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        if (status == MatchmakingStatus.SEARCHING) {
            Spacer(Modifier.height(32.dp))
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            Row(
                modifier = Modifier.width(280.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerBox(value = "%02d".format(minutes), label = "Minutes", modifier = Modifier.weight(1f))
                Text(":", style = AppTypography.headlineMedium, color = GoldPrimary)
                TimerBox(value = "%02d".format(seconds), label = "Seconds", modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(32.dp))
        if (status == MatchmakingStatus.IDLE) {
            Button(
                onClick = onStartMatchmaking,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp).fillMaxWidth(0.7f)
            ) {
                Text("⚔  FIND MATCH", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold)
            }
        } else if (status == MatchmakingStatus.SEARCHING) {
            Button(
                onClick = onCancelMatchmaking,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.height(48.dp).fillMaxWidth(0.7f)
            ) {
                Text("✕  CANCEL MATCHMAKING", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimerBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(value, style = AppTypography.displayLarge, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label.uppercase(), style = AppTypography.labelSmall, color = GoldPrimary.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QueueStabilitySection(
    queuePosition: Int,
    estimatedWaitSeconds: Int,
    isSearching: Boolean
) {
    if (!isSearching) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("QUEUE POSITION", style = AppTypography.labelSmall, color = GoldPrimary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("#$queuePosition", style = AppTypography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Estimated wait: ${estimatedWaitSeconds}s",
            style = AppTypography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ActiveRoomsHeader(roomCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ACTIVE ROOMS ($roomCount)", style = AppTypography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text("View All", style = AppTypography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RoomCard(
    room: BattleRoom,
    isGuest: Boolean,
    onJoin: () -> Unit,
    onWatch: () -> Unit
) {
    val isPlayingRoom = room.isPlayingRoom
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(
                if (isPlayingRoom) Color(0xFF7B1FA2).copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (isPlayingRoom) Color(0xFF7B1FA2).copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(
                if (isPlayingRoom) Color(0xFF7B1FA2).copy(alpha = 0.2f)
                else GoldPrimary.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center
        ) { Text(if (isPlayingRoom) "👁" else "⚔", fontSize = 20.sp) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(room.name, style = AppTypography.titleMedium, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val occupancy =  "${room.playingCount}/2"
                Text("👤 $occupancy", style = AppTypography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                Spacer(Modifier.width(12.dp))
                Text(
                    "⏱ ${room.timeControlSeconds / 60}m",
                    style = AppTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (isPlayingRoom) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "● LIVE",
                        style = AppTypography.labelSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (isPlayingRoom) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "${room.host?.name} vs ${room.guest?.name}",
                    style = AppTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
        if (isPlayingRoom) {
            Button(
                onClick = onWatch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B1FA2).copy(alpha = 0.7f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("WATCH", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold) }
        } else if (isGuest) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "SIGN IN",
                    style = AppTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else if (room.private) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "PRIVATE",
                    style = AppTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("JOIN", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CreateRoomSection(onCreateRoom: () -> Unit, onJoinById: () -> Unit, isGuest: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCreateRoom,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGuest) GoldPrimary.copy(alpha = 0.3f) else GoldPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text("＋  CREATE PRIVATE ROOM", style = AppTypography.labelLarge, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onJoinById,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
        ) {
            Text("🔗  JOIN BY ROOM ID", style = AppTypography.labelLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
