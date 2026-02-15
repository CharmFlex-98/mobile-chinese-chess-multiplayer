package com.charmflex.xiangqi.engine.ai

import com.charmflex.xiangqi.engine.model.*
import kotlin.random.Random

object ZobristHash {

    private val table: Array<Array<Array<LongArray>>>
    private val sideToMove: Long

    init {
        val rng = Random(0x12345678L)
        table = Array(2) { Array(7) { Array(10) { LongArray(9) { rng.nextLong() } } } }
        sideToMove = rng.nextLong()
    }

    fun hash(board: Board, currentTurn: PieceColor): Long {
        var h = 0L
        for (row in 0 until Board.ROWS) {
            for (col in 0 until Board.COLS) {
                val piece = board[row, col] ?: continue
                h = h xor pieceHash(piece, row, col)
            }
        }
        if (currentTurn == PieceColor.BLACK) {
            h = h xor sideToMove
        }
        return h
    }

    fun pieceHash(piece: Piece, row: Int, col: Int): Long {
        val colorIndex = if (piece.color == PieceColor.RED) 0 else 1
        val typeIndex = piece.type.ordinal
        return table[colorIndex][typeIndex][row][col]
    }

    fun sideHash(): Long = sideToMove
}
