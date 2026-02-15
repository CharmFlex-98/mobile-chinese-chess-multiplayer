package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun SocialScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SocialHeader()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SearchBar() }
            item { FriendTabs() }

            // Online
            item { SectionHeader("ONLINE", showPulse = true) }
            item { FriendCard("DragonMaster_88", "9-DAN", "Winning streak: 5", isOnline = true) }
            item { FriendCard("BambooStrategist", "5-DAN", "In Menu", isOnline = true) }

            // Offline
            item { SectionHeader("OFFLINE", showPulse = false) }
            item { FriendCard("RedGeneral_Xi", "", "Last seen 2h ago", isOnline = false) }
            item { FriendCard("Hidden_Dragon_22", "", "Last seen 1d ago", isOnline = false) }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun SocialHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♟", fontSize = 20.sp, color = GoldPrimary)
            Spacer(Modifier.width(8.dp))
            Text("HALL OF FRIENDS", style = AppTypography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier.size(40.dp).background(GoldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("＋", fontSize = 20.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SearchBar() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Search by ID or Username...", color = TextSlate500, fontSize = 14.sp) },
        leadingIcon = { Text("🔍", fontSize = 16.sp) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CardDark,
            unfocusedContainerColor = CardDark,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
private fun FriendTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(GoldPrimary, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Friends (24)", style = AppTypography.labelLarge, color = Color.Black)
        }
        Row(
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Requests", style = AppTypography.labelLarge, color = TextSlate400)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(20.dp).background(GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("3", style = AppTypography.labelSmall, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, showPulse: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, style = AppTypography.labelSmall, color = TextSlate500, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        if (showPulse) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.size(8.dp).background(OnlineGreen, CircleShape))
        }
    }
}

@Composable
private fun FriendCard(name: String, rank: String, status: String, isOnline: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isOnline) CardDark else CardDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, if (isOnline) GoldPrimary.copy(alpha = 0.05f) else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isOnline) GoldAccent else TextSlate600, CircleShape)
                    .background(AccentDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.first().toString(),
                    color = if (isOnline) GoldPrimary else TextSlate600,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(OnlineGreen, CircleShape)
                        .border(2.dp, CardDark, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = AppTypography.titleMedium, color = if (isOnline) Color.White else TextSlate400, fontSize = 14.sp)
                if (rank.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = GoldAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f))
                    ) {
                        Text(rank, style = AppTypography.labelSmall, color = GoldAccent, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(status, style = AppTypography.bodyMedium, color = TextSlate500, fontSize = 12.sp)
        }

        if (isOnline) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("INVITE", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold)
            }
        } else {
            Surface(color = AccentDark, shape = RoundedCornerShape(8.dp)) {
                Text("OFFLINE", style = AppTypography.labelSmall, color = TextSlate500, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}
