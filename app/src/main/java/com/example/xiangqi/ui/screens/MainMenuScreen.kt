package com.example.xiangqi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xiangqi.ui.ChinesePatternBackground
import com.example.xiangqi.ui.theme.*

@Composable
fun MainMenuScreen() {
    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = { /* Hosted in MainActivity */ }
    ) { padding ->
        ChinesePatternBackground(
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { Spacer(Modifier.height(16.dp)) }

                // Profile Header
                item { ProfileHeader() }

                // Progress Bar
                item { XPProgressBar() }

                // Search Section
                item { SearchSection() }

                // Main Navigation Grid
                item { NavigationGrid() }

                // Recent Activities
                item { RecentActivities() }

                item { Spacer(Modifier.height(100.dp)) } // Space for bottom nav
            }
        }
    }
}

@Composable
fun ProfileHeader() {
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
                        .background(AccentDark)
                ) {
                    // Avatar placeholder
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        tint = GoldPrimary
                    )
                }
                Surface(
                    color = GoldPrimary,
                    shape = RoundedCornerShape(full = 10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .border(2.dp, BackgroundDark, RoundedCornerShape(10.dp))
                ) {
                    Text(
                        "LV 42",
                        style = Typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Grandmaster Player",
                    style = Typography.titleMedium,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Rating: 1850",
                        style = Typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        " • ",
                        color = TextSlate400,
                        fontSize = 12.sp
                    )
                    Text(
                        "PRO LEAGUE",
                        style = Typography.labelSmall,
                        color = TextSlate400,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        IconButton(
            onClick = {},
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceDark, RoundedCornerShape(8.dp))
                .border(1.dp, AccentDark, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun XPProgressBar() {
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
            Text(
                "SEASON PROGRESS",
                style = Typography.labelSmall,
                color = TextSlate400,
                letterSpacing = 1.sp
            )
            Text(
                "750 / 1000 XP",
                style = Typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
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
fun SearchSection() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Search Room ID or Player Name...", color = TextSlate500, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSlate400) },
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
fun NavigationGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Online Duel Featured
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(GoldPrimary, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            // Pattern Overlay placeholder
            Icon(
                Icons.Default.SportsEsports,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp),
                tint = Color.White.copy(alpha = 0.2f)
            )

            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(full = 10.dp)
                    ) {
                        Text(
                            "LIVE MATCHMAKING",
                            style = Typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ONLINE DUEL",
                        style = Typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Challenge masters worldwide",
                        style = Typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PLAY NOW",
                        style = Typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GridNavItem(
                title = "AI Practice",
                subtitle = "8 Difficulty Levels",
                icon = Icons.Default.SmartToy,
                modifier = Modifier.weight(1f)
            )
            GridNavItem(
                title = "Friends",
                subtitle = "12 Online Now",
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f),
                hasBadge = true
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
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = GoldPrimary)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Global Rankings", style = Typography.titleMedium, color = Color.White)
                    Text("Current: Top 2% (Rank #412)", style = Typography.bodySmall, color = TextSlate500)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSlate600)
        }
    }
}

@Composable
fun GridNavItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false
) {
    Box(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, AccentDark, RoundedCornerShape(12.dp))
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
                Icon(icon, contentDescription = null, tint = GoldPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = Typography.titleMedium, color = Color.White)
            Text(subtitle, style = Typography.bodySmall, color = TextSlate500, fontSize = 10.sp)
        }
    }
}

@Composable
fun RecentActivities() {
    Column {
        Text(
            "RECENT ACTIVITIES",
            style = Typography.labelSmall,
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
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("Victory vs. Chen_X", style = Typography.bodyMedium, color = Color.White)
                }
                Text("2h ago", style = Typography.labelSmall, color = TextSlate500)
            }
        }
    }
}
