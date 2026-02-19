package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*

@Composable
fun SettingsScreen(
    isGuest: Boolean = false,
    onSignOut: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("*", fontSize = 20.sp, color = GoldPrimary)
            Spacer(Modifier.width(8.dp))
            Text("SETTINGS", style = AppTypography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item { SettingsSection("GAME") }
            item { SettingsToggle("Sound Effects", true) }
            item { SettingsToggle("Vibration", true) }
            item { SettingsToggle("Show Move Hints", true) }
            item { SettingsToggle("Auto-rotate Board", false) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SettingsSection("DISPLAY") }
            item { SettingsToggle("Dark Mode", true) }
            item { SettingsToggle("Show Coordinates", false) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SettingsSection("ACCOUNT") }
            item { SettingsRow("Profile", "Edit") }
            item { SettingsRow("Language", "English") }
            item { SettingsRow("About", "v1.0.0") }

            item { Spacer(Modifier.height(24.dp)) }
            item {
                if (isGuest) {
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Text("Sign Out", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        style = AppTypography.labelSmall,
        color = GoldPrimary,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggle(label: String, defaultValue: Boolean) {
    var checked by remember { mutableStateOf(defaultValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, AccentDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = AppTypography.bodyLarge, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GoldPrimary,
                uncheckedThumbColor = TextSlate400,
                uncheckedTrackColor = AccentDark
            )
        )
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, AccentDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = AppTypography.bodyLarge, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = AppTypography.bodyMedium, color = TextSlate400)
            Spacer(Modifier.width(8.dp))
            Text(">", color = TextSlate600, fontSize = 18.sp)
        }
    }
}
