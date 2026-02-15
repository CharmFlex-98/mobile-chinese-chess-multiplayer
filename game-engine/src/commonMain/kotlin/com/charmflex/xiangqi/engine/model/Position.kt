package com.charmflex.xiangqi.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..9 && col in 0..8

    fun isInPalace(color: PieceColor): Boolean {
        val rowRange = if (color == PieceColor.BLACK) 0..2 else 7..9
        return row in rowRange && col in 3..5
    }

    fun isOnSide(color: PieceColor): Boolean {
        return if (color == PieceColor.BLACK) row in 0..4 else row in 5..9
    }

    fun hasPassedRiver(color: PieceColor): Boolean {
        return if (color == PieceColor.RED) row in 0..4 else row in 5..9
    }

    operator fun plus(other: Position): Position = Position(row + other.row, col + other.col)
}
