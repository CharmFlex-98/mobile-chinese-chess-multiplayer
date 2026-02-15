package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.gameroom.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.xiangqi.engine.model.Move

@Composable
fun MoveLog(
    moves: List<Move>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty() && expanded) {
            listState.animateScrollToItem(moves.size - 1)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Toggle button (always visible)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark.copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "LOG ▼" else "LOG ▶",
                style = AppTypography.labelSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // Expandable log panel
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(72.dp)
                    .height(180.dp)
                    .background(SurfaceDark.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(6.dp))

                if (moves.isEmpty()) {
                    Text(
                        "No moves",
                        style = AppTypography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(moves) { index, move ->
                            val isLast = index == moves.size - 1
                            Text(
                                "${index + 1}. ${move.piece.displayChar}",
                                style = AppTypography.labelSmall,
                                color = if (isLast) GoldPrimary else Color.White.copy(alpha = 0.4f),
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
