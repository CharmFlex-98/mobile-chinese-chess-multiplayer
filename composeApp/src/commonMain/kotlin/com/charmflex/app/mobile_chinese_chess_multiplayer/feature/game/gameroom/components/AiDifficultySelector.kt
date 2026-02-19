package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.xiangqi.engine.ai.AiDifficulty

@Composable
fun AiDifficultySelector(
    onSelect: (AiDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SELECT DIFFICULTY",
            style = AppTypography.labelLarge,
            color = GoldPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AiDifficulty.entries.toList()) { difficulty ->
                DifficultyCard(difficulty = difficulty, onClick = { onSelect(difficulty) })
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: AiDifficulty,
    onClick: () -> Unit
) {
    val isTimeBased = difficulty.timeLimitMs > 0
    val tierColor = if (isTimeBased) {
        Color(0xFFEC4899) // pink — time-based legendary tier
    } else when {
        difficulty.depth <= 2 -> Color(0xFF22C55E) // green
        difficulty.depth <= 4 -> GoldPrimary       // gold
        difficulty.depth <= 6 -> Color(0xFFEF4444) // red
        else -> Color(0xFF9333EA)                  // purple
    }

    val subtitle = if (isTimeBased) {
        "${difficulty.timeLimitMs / 1000}s think"
    } else {
        "Depth ${difficulty.depth}"
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, tierColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            difficulty.label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = AppTypography.labelSmall,
            color = tierColor
        )
    }
}
