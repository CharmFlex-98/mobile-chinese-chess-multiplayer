package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!expanded) {
            // Floating chat button — above system navigation bar
            Button(
                onClick = { expanded = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("\uD83D\uDCAC", fontSize = 22.sp)
            }
        } else {
            // Full-height chat overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceDark.copy(alpha = 0.97f))
                    .statusBarsPadding()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chat", style = AppTypography.titleMedium, color = GoldPrimary)
                    TextButton(onClick = { expanded = false }) {
                        Text("\u2715", color = Color.White, fontSize = 18.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Scrollable message list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Input row — stays above nav bar (and above keyboard via imePadding on parent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isMe = msg.isFromMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            if (!isMe) {
                Text(
                    text = msg.senderName,
                    style = AppTypography.labelSmall,
                    color = GoldPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        color = if (isMe) GoldPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMe) 12.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg.message,
                    style = AppTypography.bodyMedium,
                    color = if (isMe) GoldPrimary else Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
