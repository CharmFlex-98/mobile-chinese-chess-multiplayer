package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.game.gameroom.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.theme.*
import com.charmflex.xiangqi.engine.model.*

@Composable
fun XiangqiBoard(
    board: Board,
    selectedPosition: Position?,
    validMoves: List<Position>,
    lastMove: Move?,
    onTap: (Position) -> Unit,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false
) {
    // Helper to map logical board position to display position
    fun displayRow(row: Int): Int = if (isFlipped) 9 - row else row
    fun displayCol(col: Int): Int = if (isFlipped) 8 - col else col
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 10.5f)
            .background(BoardBackground, RoundedCornerShape(12.dp))
            .border(4.dp, BoardBorder, RoundedCornerShape(12.dp))
    ) {
        val paddingX = 24.dp
        val paddingY = 24.dp
        val cellWidth = (maxWidth - paddingX * 2) / 8
        val cellHeight = (maxHeight - paddingY * 2) / 9

        // Layer 1: Board grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val lineColor = BoardLineColor
            val px = paddingX.toPx()
            val py = paddingY.toPx()
            val bw = size.width - 2 * px
            val bh = size.height - 2 * py
            val cw = bw / 8
            val ch = bh / 9

            // Horizontal lines
            for (i in 0..9) {
                drawLine(lineColor, Offset(px, py + i * ch), Offset(px + bw, py + i * ch), strokeWidth)
            }

            // Vertical lines (broken by river for inner lines)
            for (i in 0..8) {
                if (i == 0 || i == 8) {
                    drawLine(lineColor, Offset(px + i * cw, py), Offset(px + i * cw, py + bh), strokeWidth)
                } else {
                    drawLine(lineColor, Offset(px + i * cw, py), Offset(px + i * cw, py + 4 * ch), strokeWidth)
                    drawLine(lineColor, Offset(px + i * cw, py + 5 * ch), Offset(px + i * cw, py + 9 * ch), strokeWidth)
                }
            }

            // Palace diagonals - top (black)
            drawLine(lineColor, Offset(px + 3 * cw, py), Offset(px + 5 * cw, py + 2 * ch), strokeWidth)
            drawLine(lineColor, Offset(px + 5 * cw, py), Offset(px + 3 * cw, py + 2 * ch), strokeWidth)
            // Palace diagonals - bottom (red)
            drawLine(lineColor, Offset(px + 3 * cw, py + 7 * ch), Offset(px + 5 * cw, py + 9 * ch), strokeWidth)
            drawLine(lineColor, Offset(px + 5 * cw, py + 7 * ch), Offset(px + 3 * cw, py + 9 * ch), strokeWidth)
        }

        // Layer 2: River text
        Text(
            text = "楚河           漢界",
            color = BoardLineColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(cellHeight * 0.5f))
        )

        // Layer 3: Last move highlight
        if (lastMove != null) {
            for (pos in listOf(lastMove.from, lastMove.to)) {
                val dRow = displayRow(pos.row)
                val dCol = displayCol(pos.col)
                val highlightSize = cellWidth.coerceAtMost(cellHeight) * 0.85f
                Box(
                    modifier = Modifier
                        .size(highlightSize)
                        .offset(
                            x = paddingX + cellWidth * dCol - highlightSize / 2,
                            y = paddingY + cellHeight * dRow - highlightSize / 2
                        )
                        .border(1.5.dp, GoldPrimary.copy(alpha = 0.3f), CircleShape)
                )
            }
        }

        // Layer 4: Valid move indicators (non-capture)
        for (pos in validMoves) {
            val isCapture = board[pos] != null
            if (!isCapture) {
                val dRow = displayRow(pos.row)
                val dCol = displayCol(pos.col)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .offset(
                            x = paddingX + cellWidth * dCol - 6.dp,
                            y = paddingY + cellHeight * dRow - 6.dp
                        )
                        .background(GoldPrimary.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        // Layer 5: Selected piece highlight
        if (selectedPosition != null) {
            val dRow = displayRow(selectedPosition.row)
            val dCol = displayCol(selectedPosition.col)
            val pulseAnim = rememberInfiniteTransition()
            val pulseAlpha by pulseAnim.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
            )
            val highlightSize = cellWidth.coerceAtMost(cellHeight) * 0.85f
            Box(
                modifier = Modifier
                    .size(highlightSize)
                    .offset(
                        x = paddingX + cellWidth * dCol - highlightSize / 2,
                        y = paddingY + cellHeight * dRow - highlightSize / 2
                    )
                    .border(2.dp, GoldPrimary.copy(alpha = pulseAlpha), CircleShape)
            )
        }

        // Layer 6: Pieces
        val pieceSize = cellWidth.coerceAtMost(cellHeight) * 0.78f
        for (row in 0 until Board.ROWS) {
            for (col in 0 until Board.COLS) {
                val piece = board[row, col] ?: continue
                val dRow = displayRow(row)
                val dCol = displayCol(col)
                val isValidCapture = Position(row, col) in validMoves
                PieceComposable(
                    piece = piece,
                    isValidCapture = isValidCapture,
                    size = pieceSize,
                    modifier = Modifier.offset(
                        x = paddingX + cellWidth * dCol - pieceSize / 2,
                        y = paddingY + cellHeight * dRow - pieceSize / 2
                    )
                )
            }
        }

        // Layer 7: Transparent touch overlay on top of everything
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped) {
                    detectTapGestures { offset ->
                        val px = paddingX.toPx()
                        val py = paddingY.toPx()
                        val cw = (size.width - 2 * px) / 8
                        val ch = (size.height - 2 * py) / 9

                        val displayCol = ((offset.x - px + cw / 2) / cw).toInt()
                        val displayRow = ((offset.y - py + ch / 2) / ch).toInt()

                        // Convert display coordinates back to logical board coordinates
                        val row = if (isFlipped) 9 - displayRow else displayRow
                        val col = if (isFlipped) 8 - displayCol else displayCol

                        if (row in 0..9 && col in 0..8) {
                            onTap(Position(row, col))
                        }
                    }
                }
        )
    }
}

@Composable
fun PieceComposable(
    piece: Piece,
    isValidCapture: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isValidCapture) {
        Color.Red.copy(alpha = 0.8f)
    } else if (piece.isRed) {
        GoldPrimary
    } else {
        Color.Gray
    }
    val borderWidth = if (isValidCapture) 2.5.dp else 2.dp

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (piece.isRed) PieceRedBackground else PieceBlackBackground,
        shadowElevation = 4.dp,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = piece.displayChar,
                color = if (piece.isRed) GoldPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.5f).sp
            )
        }
    }
}
