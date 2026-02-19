package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.GoldPrimary

@Composable
fun ChinesePatternBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val dotRadius = 1.dp.toPx()
                val spacing = 24.dp.toPx()
                val color = GoldPrimary.copy(alpha = 0.05f)

                var x = 2.dp.toPx()
                while (x < size.width) {
                    var y = 2.dp.toPx()
                    while (y < size.height) {
                        drawCircle(color = color, radius = dotRadius, center = Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
            }
    ) {
        content()
    }
}
