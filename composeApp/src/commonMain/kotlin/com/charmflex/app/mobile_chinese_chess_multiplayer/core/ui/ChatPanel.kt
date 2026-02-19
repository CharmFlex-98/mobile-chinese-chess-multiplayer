package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.charmflex.xiangqi.engine.model.ChatMessage

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    onSend: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var chatText by remember { mutableStateOf("") }

    if (!expanded) {
        // Floating chat button
        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("\uD83D\uDCAC", fontSize = 20.sp)
            }
        }
    } else {
        // Chat overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .background(SurfaceDark.copy(alpha = 0.95f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chat", style = AppTypography.titleMedium, color = GoldPrimary)
                    TextButton(onClick = { expanded = false }) {
                        Text("\u2715", color = Color.White)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                ) {
                    messages.forEach { msg ->
                        Text(
                            "${if (msg.isFromMe) "You" else msg.senderName}: ${msg.message}",
                            style = AppTypography.bodyMedium,
                            color = if (msg.isFromMe) GoldPrimary else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type message...", color = Color.White.copy(alpha = 0.3f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (chatText.isNotBlank()) {
                                onSend(chatText.trim())
                                chatText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
