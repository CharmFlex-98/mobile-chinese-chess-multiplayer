package com.charmflex.xiangqi.engine.ai

import com.charmflex.xiangqi.engine.model.*

object Evaluator {

    private val PIECE_VALUES = intArrayOf(
        10000, // GENERAL
        200,   // ADVISOR
        200,   // ELEPHANT
        400,   // HORSE
        900,   // CHARIOT
        450,   // CANNON
        100    // SOLDIER
    )

    fun pieceValue(type: PieceType): Int = PIECE_VALUES[type.ordinal]

    private val CHARIOT_TABLE = arrayOf(
        intArrayOf(14, 14, 12, 18, 16, 18, 12, 14, 14),
        intArrayOf(16, 20, 18, 24, 26, 24, 18, 20, 16),
        intArrayOf(12, 12, 12, 18, 18, 18, 12, 12, 12),
        intArrayOf(12, 18, 16, 22, 22, 22, 16, 18, 12),
        intArrayOf(12, 14, 12, 18, 18, 18, 12, 14, 12),
        intArrayOf(12, 16, 14, 20, 20, 20, 14, 16, 12),
        intArrayOf(6,  10, 8,  14, 14, 14, 8,  10,  6),
        intArrayOf(4,  8,  6,  14, 12, 14, 6,  8,   4),
        intArrayOf(8,  4,  8,  16, 8,  16, 8,  4,   8),
        intArrayOf(-2, 10, 6,  14, 12, 14, 6,  10, -2)
    )

    private val HORSE_TABLE = arrayOf(
        intArrayOf(4,  8,  16, 12, 4,  12, 16, 8,  4),
        intArrayOf(4,  10, 28, 16, 8,  16, 28, 10, 4),
        intArrayOf(12, 14, 16, 20, 18, 20, 16, 14, 12),
        intArrayOf(8,  24, 18, 24, 20, 24, 18, 24, 8),
        intArrayOf(6,  16, 14, 18, 16, 18, 14, 16, 6),
        intArrayOf(4,  12, 16, 14, 12, 14, 16, 12, 4),
        intArrayOf(2,  6,  8,  6,  10, 6,  8,  6,  2),
        intArrayOf(4,  2,  8,  8,  4,  8,  8,  2,  4),
        intArrayOf(0,  2,  4,  4, -2,  4,  4,  2,  0),
        intArrayOf(0, -4,  0,  0,  0,  0,  0, -4,  0)
    )

    private val CANNON_TABLE = arrayOf(
        intArrayOf(6,  4,  0, -10, -12, -10, 0,  4,  6),
        intArrayOf(2,  2,  0,  -4,  -14, -4, 0,  2,  2),
        intArrayOf(2,  2,  0, -10, -8,  -10, 0,  2,  2),
        intArrayOf(0,  0, -2,  4,   10,  4, -2,  0,  0),
        intArrayOf(0,  0,  0,  2,   8,   2,  0,  0,  0),
        intArrayOf(-2, 0,  4,  2,   6,   2,  4,  0, -2),
        intArrayOf(0,  0,  0,  2,   4,   2,  0,  0,  0),
        intArrayOf(4,  0,  8,  6,  10,   6,  8,  0,  4),
        intArrayOf(0,  2,  4,  6,   6,   6,  4,  2,  0),
        intArrayOf(0,  0,  2,  6,   6,   6,  2,  0,  0)
    )

    private val SOLDIER_TABLE = arrayOf(
        intArrayOf(0,  3,  6,  9,  12, 9,  6,  3,  0),
        intArrayOf(18, 36, 56, 80, 120,80, 56, 36, 18),
        intArrayOf(14, 26, 42, 60, 80, 60, 42, 26, 14),
        intArrayOf(10, 20, 30, 34, 40, 34, 30, 20, 10),
        intArrayOf(6,  12, 18, 18, 20, 18, 18, 12, 6),
        intArrayOf(2,  0,  8,  0,  8,  0,  8,  0,  2),
        intArrayOf(0,  0, -2,  0,  4,  0, -2,  0,  0),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val ADVISOR_TABLE = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 20, 0, 20, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val ELEPHANT_TABLE = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 20, 0, 0, 0, 20, 0, 0),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(18, 0, 0, 0, 24, 0, 0, 0, 18),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 0, 20, 0, 0, 0, 20, 0, 0)
    )

    fun evaluate(board: Board, perspective: PieceColor): Int {
        var score = 0
        var myAdvisors = 0
        var myElephants = 0
        var oppAdvisors = 0
        var oppElephants = 0

        for (row in 0 until Board.ROWS) {
            for (col in 0 until Board.COLS) {
                val piece = board[row, col] ?: continue
                val materialValue = PIECE_VALUES[piece.type.ordinal]
                val positionalValue = getPositionalValue(piece, row, col)
                val pieceScore = materialValue + positionalValue

                if (piece.color == perspective) {
                    score += pieceScore
                    when (piece.type) {
                        PieceType.ADVISOR -> myAdvisors++
                        PieceType.ELEPHANT -> myElephants++
                        else -> {}
                    }
                } else {
                    score -= pieceScore
                    when (piece.type) {
                        PieceType.ADVISOR -> oppAdvisors++
                        PieceType.ELEPHANT -> oppElephants++
                        else -> {}
                    }
                }
            }
        }

        score += kingSafety(myAdvisors, myElephants)
        score -= kingSafety(oppAdvisors, oppElephants)

        return score
    }

    private fun kingSafety(advisors: Int, elephants: Int): Int {
        val advisorBonus = advisors * 15
        val elephantBonus = elephants * 10
        val structureBonus = if (advisors == 2 && elephants == 2) 40
                            else if (advisors == 2 || elephants == 2) 15
                            else 0
        return advisorBonus + elephantBonus + structureBonus
    }

    private fun getPositionalValue(piece: Piece, row: Int, col: Int): Int {
        val r = if (piece.color == PieceColor.RED) row else 9 - row

        return when (piece.type) {
            PieceType.CHARIOT -> CHARIOT_TABLE[r][col]
            PieceType.HORSE -> HORSE_TABLE[r][col]
            PieceType.CANNON -> CANNON_TABLE[r][col]
            PieceType.SOLDIER -> SOLDIER_TABLE[r][col]
            PieceType.ADVISOR -> ADVISOR_TABLE[r][col]
            PieceType.ELEPHANT -> ELEPHANT_TABLE[r][col]
            PieceType.GENERAL -> 0
        }
    }
}
