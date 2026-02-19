package com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun SGSnackBar(
    modifier: Modifier = Modifier,
    snackBarHostState: SnackbarHostState,
    snackBarType: SnackBarType
) {
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(grid_x1)
    ) {
        SnackbarHost(hostState = snackBarHostState) {
            Snackbar(
                modifier = modifier,
                snackbarData = it,
                containerColor = SnackBarType.containerColor(snackBarType = snackBarType),
                contentColor = SnackBarType.textColor(snackBarType = snackBarType),
                shape = RoundedCornerShape(grid_x1_5)
            )
        }
    }

}

sealed interface SnackBarType {
    object Success : SnackBarType
    object Error : SnackBarType

    companion object {
        @Composable
        fun containerColor(snackBarType: SnackBarType): Color {
            return when (snackBarType) {
                Success -> greenColor
                Error -> redColor
            }
        }

        fun textColor(snackBarType: SnackBarType): Color {
            return when (snackBarType) {
                Success -> Color.Black
                Error -> Color.White
            }
        }
    }
}

suspend fun SnackbarHostState.showSnackBarImmediately(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message = message, duration = SnackbarDuration.Short)
    delay(500)
}

val redColor = Color(181, 32, 32)
val greenColor = Color(61, 165, 1)