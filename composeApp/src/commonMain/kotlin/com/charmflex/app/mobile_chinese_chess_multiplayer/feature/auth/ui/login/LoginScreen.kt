package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.ChinesePatternBackground

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
) {
    val state by viewModel.state.collectAsState()

    ChinesePatternBackground(
        modifier = Modifier.background(BackgroundDark)
    ) {
        if (state.isRestoringSession) {
            Box(
                modifier = Modifier.fillMaxSize().background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
            return@ChinesePatternBackground
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // App title
            Text(
                text = "\u8C61\u68CB\u5927\u5E2B",
                fontSize = 48.sp,
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "XIANGQI MASTER",
                style = AppTypography.titleMedium,
                color = GoldPrimary.copy(alpha = 0.7f),
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            // Sign in with Google button
            Button(
                onClick = viewModel::signInWithGoogle,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "SIGN IN WITH GOOGLE",
                        style = AppTypography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error message
            state.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF5252),
                    style = AppTypography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
