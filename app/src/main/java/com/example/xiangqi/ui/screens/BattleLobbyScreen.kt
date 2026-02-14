package com.example.xiangqi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xiangqi.ui.theme.*

@Composable
fun BattleLobbyScreen() {
    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = { /* Hosted in MainActivity */ }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LobbyHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item { MatchmakingRadarSection() }
                item { QueueStabilitySection() }
                item { ActiveRoomsSection() }
                item { CreateRoomSection() }
            }
        }
    }
}

@Composable
fun LobbyHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark.copy(alpha = 0.95f))
            .border(bottom = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = GoldPrimary)
            Spacer(Modifier.width(12.dp))
            Text("BATTLE LOBBY", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = GoldPrimary)
            }
        }
    }
}

@Composable
fun MatchmakingRadarSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GoldPrimary.copy(alpha = 0.05f), Color.Transparent)
                )
            )
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
            )

            // Pulse Rings
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .border(1.dp, GoldPrimary.copy(alpha = alpha), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.1f), CircleShape)
            )

            // Central Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("Searching for Opponent...", style = Typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text("RANK: MASTER TIER", style = Typography.labelSmall, color = GoldPrimary.copy(alpha = 0.7f), letterSpacing = 1.sp)

        Spacer(Modifier.height(32.dp))

        // Timer
        Row(
            modifier = Modifier.width(280.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerBox(value = "01", label = "Minutes", modifier = Modifier.weight(1f))
            Text(":", style = Typography.headlineSmall, color = GoldPrimary)
            TimerBox(value = "24", label = "Seconds", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.height(48.dp).fillMaxWidth(0.7f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("CANCEL MATCHMAKING", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TimerBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(value, style = Typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label.uppercase(), style = Typography.labelSmall, color = GoldPrimary.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QueueStabilitySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .border(top = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), bottom = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("QUEUE STABILITY", style = Typography.labelSmall, color = GoldPrimary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("70%", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight()
                    .background(GoldPrimary, CircleShape)
            )
        }
    }
}

@Composable
fun ActiveRoomsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACTIVE ROOMS (12)", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text("View All", style = Typography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RoomCard(name = "Dragon's Peak", tier = "PRO", tierColor = GoldPrimary, occupancy = "1/2", ping = "24ms", icon = Icons.Default.SportsScore)
            RoomCard(name = "Elite Scrims", tier = "ELITE", tierColor = Color.White.copy(alpha = 0.7f), occupancy = "3/4", ping = "48ms", icon = Icons.Default.EmojiEvents)
            RoomCard(name = "Neon City Rush", tier = "GOLD", tierColor = Color(0xFFEAB308), occupancy = "1/2", ping = "12ms", icon = Icons.Default.FlashOn)
        }
    }
}

@Composable
fun RoomCard(name: String, tier: String, tierColor: Color, occupancy: String, ping: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GoldPrimary)
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Typography.titleSmall, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = tierColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.2f))
                ) {
                    Text(tier, style = Typography.labelSmall, color = tierColor, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Black, fontSize = 8.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Text(occupancy, style = Typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Text(ping, style = Typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
        }

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("JOIN", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CreateRoomSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddBox, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("CREATE PRIVATE ROOM", style = Typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
