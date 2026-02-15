package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.common.ChinesePatternBackground

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel? = null,
    onNavigateToGame: () -> Unit = {},
    onNavigateToAi: () -> Unit = {},
    onNavigateToSocial: () -> Unit = {},
    onNavigateToBattle: () -> Unit = {}
) {
    val menuState = viewModel?.state?.collectAsState()?.value

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
                    playerName = menuState?.playerName ?: "Guest",
                    playerRating = menuState?.playerRating ?: 1200
                )
            }
            item { XPProgressBar() }
            item { SearchSection() }
            item {
                NavigationGrid(
                    onOnlineDuel = onNavigateToBattle,
                    onAiPractice = onNavigateToAi,
                    onFriends = onNavigateToSocial
                )
            }
            item { RecentActivities() }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun ProfileHeader(
    playerName: String = "Guest",
    playerRating: Int = 1200
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldPrimary, CircleShape)
                        .background(AccentDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text("帥", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
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
                        "LV 42",
                        style = AppTypography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(playerName, style = AppTypography.titleMedium, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rating: $playerRating", style = AppTypography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                    Text(" \u2022 ", color = TextSlate400, fontSize = 12.sp)
                    Text("PRO LEAGUE", style = AppTypography.labelSmall, color = TextSlate400, letterSpacing = 1.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceDark, RoundedCornerShape(8.dp))
                .border(1.dp, AccentDark, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔔", fontSize = 18.sp)
        }
    }
}

@Composable
private fun XPProgressBar() {
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
            Text("SEASON PROGRESS", style = AppTypography.labelSmall, color = TextSlate400, letterSpacing = 1.sp)
            Text("750 / 1000 XP", style = AppTypography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
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
                    .fillMaxWidth(0.75f)
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
    onFriends: () -> Unit
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GridNavItem(
                title = "AI Practice",
                subtitle = "8 Difficulty Levels",
                symbol = "🤖",
                modifier = Modifier.weight(1f),
                onClick = onAiPractice
            )
            GridNavItem(
                title = "Friends",
                subtitle = "12 Online Now",
                symbol = "👥",
                modifier = Modifier.weight(1f),
                hasBadge = true,
                onClick = onFriends
            )
        }

        // Global Rankings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, AccentDark, RoundedCornerShape(12.dp))
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
                    Text("Current: Top 2% (Rank #412)", style = AppTypography.bodyMedium, color = TextSlate500, fontSize = 12.sp)
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
