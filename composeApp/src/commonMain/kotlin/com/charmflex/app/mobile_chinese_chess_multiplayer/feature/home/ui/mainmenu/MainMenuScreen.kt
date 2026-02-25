package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.mainmenu

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ChinesePatternBackground
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.profile.Player

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onNavigateToBattle: () -> Unit = {}
) {
    val state = viewModel.state?.collectAsState()?.value

    ChinesePatternBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                ProfileHeader(
                    playerName = state?.playerName ?: "Guest",
                    xp = state?.xp ?: 0,
                    level = state?.level ?: 1,
                    avatarUrl = state?.avatarUrl,
                    isAdmin = state?.isAdmin == true,
                    onAdminClick = viewModel::onShowAdminPanel,
                    onLogoutClick = viewModel::logout
                )
            }
            item { XPProgressBar(xp = state?.xp ?: 0, level = state?.level ?: 1) }
            item {
                NavigationGrid(
                    onOnlineDuel = onNavigateToBattle,
                    onAiPractice = viewModel::onNavigateToAISelection,
                    onShowLeaderboard = viewModel::onShowLeaderboard
                )
            }
        }
    }

    if (state?.showLeaderboard == true) {
        LeaderboardDialog(
            entries = state.leaderboard,
            isLoading = state.isLeaderboardLoading,
            onDismiss = viewModel::onDismissLeaderboard
        )
    }

    state?.pendingLevelUp?.let { newLevel ->
        LevelUpDialog(newLevel = newLevel, onDismiss = viewModel::onLevelUpAcknowledged)
    }

    if (state?.showAdminPanel == true) {
        AdminChatPanel(
            rooms = state.adminRooms,
            isLoading = state.isAdminLoading,
            onSend = viewModel::onSendAdminMessage,
            onDismiss = viewModel::onDismissAdminPanel
        )
    }
}

@Composable
private fun ProfileHeader(
    playerName: String = "Guest",
    xp: Int = 0,
    level: Int = 1,
    avatarUrl: String? = null,
    isAdmin: Boolean = false,
    onLogoutClick: () -> Unit,
    onAdminClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, GoldPrimary, CircleShape)
                    .background(AccentDark),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AvatarImage(url = avatarUrl, fallbackInitial = playerName.firstOrNull()?.uppercaseChar()?.toString() ?: "帥")
                } else {
                    Text(
                        playerName.firstOrNull()?.uppercaseChar()?.toString() ?: "帥",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
            Surface(
                color = GoldPrimary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .border(2.dp, BackgroundDark, RoundedCornerShape(10.dp))
            ) {
                Text(
                    "LV $level",
                    style = AppTypography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(playerName, style = AppTypography.titleMedium, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("XP: $xp", style = AppTypography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                Text(" \u2022 ", color = TextSlate400, fontSize = 12.sp)
                Text("Level $level", style = AppTypography.labelSmall, color = TextSlate400, letterSpacing = 1.sp)
            }
        }
        if (isAdmin) {
            Surface(
                color = Color(0xFF7B1FA2).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, Color(0xFF7B1FA2).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onAdminClick)
            ) {
                Text(
                    "⚙ Admin",
                    color = Color(0xFFCE93D8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            color = Color(0xFF7B1FA2).copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .border(1.dp, Color(0xFF7B1FA2).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clickable(onClick = onLogoutClick)
        ) {
            Text(
                "Logout",
                color = Color(0xFFCE93D8),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun XPProgressBar(xp: Int = 0, level: Int = 1) {
    // XP required to go from level n to n+1: 100 + (n-1)*5
    val xpRequiredThisLevel = Player.xpToNextLevel(level)
    val xpForCurrentLevel = Player.cumulativeXpForLevel(level)
    val xpInCurrentLevel = xp - xpForCurrentLevel
    val progress = xpInCurrentLevel.toFloat() / xpRequiredThisLevel.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, AccentDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("LEVEL PROGRESS", style = AppTypography.labelSmall, color = TextSlate400, letterSpacing = 1.sp)
            Text("$xpInCurrentLevel / $xpRequiredThisLevel XP", style = AppTypography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AccentDark, CircleShape)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(GoldPrimary)
            )
        }
    }
}

@Composable
private fun SearchSection() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Search Room ID or Player Name...", color = TextSlate500, fontSize = 14.sp) },
        leadingIcon = { Text("🔍", fontSize = 16.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedIndicatorColor = GoldPrimary,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = GoldPrimary
        ),
        singleLine = true
    )
}

@Composable
private fun NavigationGrid(
    onOnlineDuel: () -> Unit,
    onAiPractice: () -> Unit,
    onShowLeaderboard: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Online Duel Featured
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GoldPrimary, RoundedCornerShape(12.dp))
                .clickable(onClick = onOnlineDuel)
                .padding(24.dp)
        ) {
            Text(
                "⚔",
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp),
                fontSize = 64.sp,
                color = Color.White.copy(alpha = 0.2f)
            )
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "LIVE MATCHMAKING",
                            style = AppTypography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("ONLINE DUEL", style = AppTypography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                    Text("Challenge masters worldwide", style = AppTypography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
                Text("PLAY NOW →", style = AppTypography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        GridNavItem(
            title = "AI Practice",
            subtitle = "8 Difficulty Levels",
            symbol = "🤖",
            modifier = Modifier.fillMaxWidth(),
            onClick = onAiPractice
        )

        // Global Rankings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, AccentDark, RoundedCornerShape(12.dp))
                .clickable(onClick = onShowLeaderboard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏆", fontSize = 20.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Global Rankings", style = AppTypography.titleMedium, color = Color.White)
                    Text("Top players by level and XP", style = AppTypography.bodyMedium, color = TextSlate500, fontSize = 12.sp)
                }
            }
            Text("›", color = TextSlate600, fontSize = 24.sp)
        }
    }
}

@Composable
private fun GridNavItem(
    title: String,
    subtitle: String,
    symbol: String,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, AccentDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(GoldPrimary, CircleShape)
                    .align(Alignment.TopEnd)
            )
        }
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = AppTypography.titleMedium, color = Color.White)
            Text(subtitle, style = AppTypography.bodyMedium, color = TextSlate500, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AvatarImage(url: String, fallbackInitial: String) {
    val context = LocalPlatformContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = fallbackInitial,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
        error = null
    //        error = {
//            Text(fallbackInitial, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
//        }
    )
}

@Composable
private fun LeaderboardDialog(
    entries: List<LeaderboardEntryUi>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆 Global Rankings", style = AppTypography.titleLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
                Text("✕", modifier = Modifier.clickable(onClick = onDismiss), color = TextSlate400, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else if (entries.isEmpty()) {
                Text("No rankings yet", color = TextSlate500, modifier = Modifier.padding(16.dp))
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        LeaderboardRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryUi) {
    val rankColor = when (entry.rank) {
        1 -> GoldPrimary
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextSlate400
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${entry.rank}",
                style = AppTypography.labelSmall,
                color = rankColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(entry.name, style = AppTypography.bodyMedium, color = Color.White)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Lv ${entry.level}", style = AppTypography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
            Text("${entry.xp} XP", style = AppTypography.labelSmall, color = TextSlate400)
        }
    }
}

@Composable
private fun AdminChatPanel(
    rooms: List<com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.BattleRoom>,
    isLoading: Boolean,
    onSend: (roomId: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRoom by remember { mutableStateOf<com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.domain.repository.BattleRoom?>(null) }
    var message by remember { mutableStateOf("") }
    var showRoomDropdown by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚙ Admin Chat", style = AppTypography.titleLarge, color = Color(0xFFCE93D8), fontWeight = FontWeight.Bold)
                Text("✕", modifier = Modifier.clickable(onClick = onDismiss), color = TextSlate400, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else if (rooms.isEmpty()) {
                Text("No active rooms", color = TextSlate500, modifier = Modifier.padding(8.dp))
            } else {
                // Room selector
                Box {
                    OutlinedButton(
                        onClick = { showRoomDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF7B1FA2).copy(alpha = 0.5f))
                    ) {
                        Text(
                            selectedRoom?.let { "${it.host?.name ?: "?"} vs ${it.guest?.name ?: "Waiting"}" }
                                ?: "Select a room…",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text("▾", color = TextSlate400)
                    }
                    DropdownMenu(
                        expanded = showRoomDropdown,
                        onDismissRequest = { showRoomDropdown = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        rooms.filter { it.status == "playing" }.forEach { room ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${room.id}",
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedRoom = room
                                    showRoomDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Message input
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Reply as Bot…", color = TextSlate500) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7B1FA2),
                        unfocusedBorderColor = Color(0xFF7B1FA2).copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val room = selectedRoom ?: return@Button
                        if (message.isNotBlank()) {
                            onSend(room.id, message.trim())
                            message = ""
                        }
                    },
                    enabled = selectedRoom != null && message.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send as Bot", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LevelUpDialog(newLevel: Int, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LEVEL UP!", style = AppTypography.headlineMedium, color = GoldPrimary, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text("You reached", style = AppTypography.bodyMedium, color = TextSlate400)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = GoldPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Text(
                    "Level $newLevel",
                    style = AppTypography.headlineMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Awesome!", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentActivities() {
    Column {
        Text(
            "RECENT ACTIVITIES",
            style = AppTypography.labelSmall,
            color = TextSlate500,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .border(1.dp, AccentDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(OnlineGreen, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("Victory vs. Chen_X", style = AppTypography.bodyMedium, color = Color.White)
                }
                Text("2h ago", style = AppTypography.labelSmall, color = TextSlate500)
            }
        }
    }
}
