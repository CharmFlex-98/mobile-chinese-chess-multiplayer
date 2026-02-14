package com.charmflex.app.mobile_chinese_chess_multiplayer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.ui.theme.*

@Composable
fun GameRoomScreen() {
    Scaffold(
        containerColor = BackgroundDarkAlt
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header: Opponent Info
            OpponentHeader()

            // Main Game Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(BackgroundDarkAlt)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                XiangqiBoard()

                // Floating Move Log
                MoveLog(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
            }

            // Chat Panel
            ChatPanel()

            // Footer: User Info & Controls
            UserFooter()

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun OpponentHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDarkAlt.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldPrimary.copy(alpha = 0.2f), CircleShape)
                        .background(AccentDarkAlt)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.fillMaxSize().padding(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(2.dp, SurfaceDarkAlt, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Zhang Wei", style = Typography.titleSmall, color = Color.White)
                Text("GRANDMASTER • 2480", style = Typography.labelSmall, color = GoldPrimary)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(
                modifier = Modifier
                    .background(AccentDarkAlt, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("08:42", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(4.dp)
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
}

@Composable
fun XiangqiBoard() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .background(Color(0xFF1D1212), RoundedCornerShape(12.dp))
            .border(4.dp, Color(0xFF3D2425), RoundedCornerShape(12.dp))
    ) {
        val boardWidthDp = maxWidth
        val boardHeightDp = maxHeight

        val paddingXDp = 24.dp
        val paddingYDp = 24.dp
        val cellWidthDp = (boardWidthDp - paddingXDp * 2) / 8
        val cellHeightDp = (boardHeightDp - paddingYDp * 2) / 9

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val lineColor = Color(0xFF5C4D33)
            val paddingX = paddingXDp.toPx()
            val paddingY = paddingYDp.toPx()
            val boardWidth = boardWidthDp.toPx() - 2 * paddingX
            val boardHeight = boardHeightDp.toPx() - 2 * paddingY
            val cellWidth = boardWidth / 8
            val cellHeight = boardHeight / 9

            // Horizontal lines
            for (i in 0..9) {
                drawLine(
                    color = lineColor,
                    start = Offset(paddingX, paddingY + i * cellHeight),
                    end = Offset(paddingX + boardWidth, paddingY + i * cellHeight),
                    strokeWidth = strokeWidth
                )
            }

            // Vertical lines
            for (i in 0..8) {
                if (i == 0 || i == 8) {
                    drawLine(
                        color = lineColor,
                        start = Offset(paddingX + i * cellWidth, paddingY),
                        end = Offset(paddingX + i * cellWidth, paddingY + boardHeight),
                        strokeWidth = strokeWidth
                    )
                } else {
                    drawLine(
                        color = lineColor,
                        start = Offset(paddingX + i * cellWidth, paddingY),
                        end = Offset(paddingX + i * cellWidth, paddingY + 4 * cellHeight),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(paddingX + i * cellWidth, paddingY + 5 * cellHeight),
                        end = Offset(paddingX + i * cellWidth, paddingY + 9 * cellHeight),
                        strokeWidth = strokeWidth
                    )
                }
            }

            // Palaces
            drawLine(lineColor, Offset(paddingX + 3 * cellWidth, paddingY), Offset(paddingX + 5 * cellWidth, paddingY + 2 * cellHeight), strokeWidth)
            drawLine(lineColor, Offset(paddingX + 5 * cellWidth, paddingY), Offset(paddingX + 3 * cellWidth, paddingY + 2 * cellHeight), strokeWidth)
            drawLine(lineColor, Offset(paddingX + 3 * cellWidth, paddingY + 7 * cellHeight), Offset(paddingX + 5 * cellWidth, paddingY + 9 * cellHeight), strokeWidth)
            drawLine(lineColor, Offset(paddingX + 5 * cellWidth, paddingY + 7 * cellHeight), Offset(paddingX + 3 * cellWidth, paddingY + 9 * cellHeight), strokeWidth)
        }

        // River Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = paddingYDp + cellHeightDp * 4.5f - 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "楚河           漢界",
                style = Typography.titleSmall,
                color = Color(0xFF5C4D33),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // Example Pieces
        Piece(
            text = "帥",
            isRed = true,
            modifier = Modifier.offset(
                x = paddingXDp + cellWidthDp * 4 - 16.dp,
                y = paddingYDp + cellHeightDp * 9 - 16.dp
            )
        )

        Piece(
            text = "將",
            isRed = false,
            modifier = Modifier.offset(
                x = paddingXDp + cellWidthDp * 4 - 16.dp,
                y = paddingYDp + cellHeightDp * 0 - 16.dp
            )
        )

        val infiniteTransition = rememberInfiniteTransition()
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
        )

        Box(
            modifier = Modifier
                .size(16.dp)
                .offset(
                    x = paddingXDp + cellWidthDp * 4 - 8.dp,
                    y = paddingYDp + cellHeightDp * 1 - 8.dp
                )
                .border(2.dp, GoldPrimary.copy(alpha = pulseAlpha), CircleShape)
        )
    }
}

@Composable
fun Piece(text: String, isRed: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = if (isRed) Color(0xFFFDF2F2) else Color(0xFF261C1C),
        border = if (isRed) BorderStroke(2.dp, GoldPrimary) else BorderStroke(2.dp, Color.Gray),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isRed) GoldPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun MoveLog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(64.dp)
            .height(200.dp)
            .background(SurfaceDark.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LOG", style = Typography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(4) { i ->
                Text("${i+1}. Move", style = Typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
            }
            item {
                Text("5. 车一平二", style = Typography.labelSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChatPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .background(AccentDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(12.dp))
            Text("Tap to send a message...", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AccentDark, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GoldPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun UserFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(2.dp, GoldPrimary, CircleShape)
                            .background(AccentDarkAlt)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.fillMaxSize().padding(8.dp))
                    }
                    Surface(
                        color = GoldPrimary,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Text("YOU", style = Typography.labelSmall, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Li Wei", style = Typography.titleSmall, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("RANK #42 CHINA", style = Typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    modifier = Modifier
                        .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("04:15", style = Typography.titleMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight()
                            .background(GoldPrimary, CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameActionButton(icon = Icons.Default.Undo, label = "Undo", modifier = Modifier.weight(1f))
            GameActionButton(icon = Icons.Default.Handshake, label = "Draw", modifier = Modifier.weight(1f))
            GameActionButton(icon = Icons.Default.Flag, label = "Resign", modifier = Modifier.weight(1f), isPrimary = true)
        }
    }
}

@Composable
fun GameActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, isPrimary: Boolean = false) {
    Column(
        modifier = modifier
            .background(
                if (isPrimary) GoldPrimary.copy(alpha = 0.1f) else AccentDark,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (isPrimary) GoldPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (isPrimary) GoldPrimary else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), style = Typography.labelSmall, color = if (isPrimary) GoldPrimary else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
    }
}
