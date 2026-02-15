package com.example.xiangqi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xiangqi.ui.ChinesePatternBackground
import com.example.xiangqi.ui.theme.*

@Composable
fun SocialScreen() {
    Scaffold(
        containerColor = BackgroundDeepDark,
        bottomBar = { /* Hosted in MainActivity */ }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SocialHeader()
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SearchBar() }
                item { FriendTabs() }
                
                // Online Section
                item {
                    SectionHeader(title = "ONLINE", showPulse = true)
                }
                items(2) { i ->
                    FriendCard(
                        name = if (i == 0) "DragonMaster_88" else "BambooStrategist",
                        rank = if (i == 0) "9-DAN" else "5-DAN",
                        status = if (i == 0) "Winning streak: 5" else "In Menu",
                        isOnline = true
                    )
                }
                
                // Offline Section
                item {
                    SectionHeader(title = "OFFLINE", showPulse = false)
                }
                items(2) { i ->
                    FriendCard(
                        name = if (i == 0) "RedGeneral_Xi" else "Hidden_Dragon_22",
                        rank = "",
                        status = if (i == 0) "Last seen 2h ago" else "Last seen 1d ago",
                        isOnline = false
                    )
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun SocialHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("HALL OF FRIENDS", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
            }
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(GoldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black)
        }
    }
}

@Composable
fun SearchBar() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Search by ID or Username...", color = TextSlate500, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSlate400) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
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
fun FriendTabs() {
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
            Text("Friends (24)", style = Typography.labelLarge, color = Color.Black)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Requests", style = Typography.labelLarge, color = TextSlate400)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("3", style = Typography.labelSmall, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, showPulse: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title, 
            style = Typography.labelSmall, 
            color = TextSlate500, 
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
        if (showPulse) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
        }
    }
}

@Composable
fun FriendCard(name: String, rank: String, status: String, isOnline: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isOnline) CardDark else CardDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(
                1.dp, 
                if (isOnline) GoldPrimary.copy(alpha = 0.05f) else Color.Transparent, 
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isOnline) GoldAccent else TextSlate600, CircleShape)
                    .background(AccentDark)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = if (isOnline) GoldPrimary else TextSlate600, modifier = Modifier.fillMaxSize().padding(8.dp))
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(2.dp, CardDark, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Typography.titleSmall, color = if (isOnline) Color.White else TextSlate400)
                if (rank.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = GoldAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f))
                    ) {
                        Text(
                            rank, 
                            style = Typography.labelSmall, 
                            color = GoldAccent, 
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Text(status, style = Typography.bodySmall, color = TextSlate500)
        }
        
        if (isOnline) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("INVITE", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Surface(
                color = AccentDark,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "OFFLINE", 
                    style = Typography.labelSmall, 
                    color = TextSlate500, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
