package com.charmflex.xiangqi.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class PieceColor {
    RED, BLACK;

    val opponent: PieceColor get() = if (this == RED) BLACK else RED
}

@Serializable
enum class PieceType {
    GENERAL,   // 帥/將
    ADVISOR,   // 仕/士
    ELEPHANT,  // 相/象
    HORSE,     // 馬
    CHARIOT,   // 車
    CANNON,    // 炮
    SOLDIER;   // 兵/卒

    fun displayChar(color: PieceColor): String = when (this) {
        GENERAL -> if (color == PieceColor.RED) "帥" else "將"
        ADVISOR -> if (color == PieceColor.RED) "仕" else "士"
        ELEPHANT -> if (color == PieceColor.RED) "相" else "象"
        HORSE -> if (color == PieceColor.RED) "馬" else "馬"
        CHARIOT -> if (color == PieceColor.RED) "車" else "車"
        CANNON -> if (color == PieceColor.RED) "炮" else "砲"
        SOLDIER -> if (color == PieceColor.RED) "兵" else "卒"
    }
}

@Serializable
data class Piece(val type: PieceType, val color: PieceColor) {
    val displayChar: String get() = type.displayChar(color)
    val isRed: Boolean get() = color == PieceColor.RED
}
