package com.charmflex.app.mobile_chinese_chess_multiplayer.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.AppTypography
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.GoldPrimary

@Composable
fun XiangqiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = GoldPrimary,
    contentColor: Color = Color.Black,
    symbol: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (symbol != null) {
                Text(symbol)
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text.uppercase(), style = AppTypography.labelLarge)
        }
    }
}
