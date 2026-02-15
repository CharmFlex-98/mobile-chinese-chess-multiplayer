package com.charmflex.xiangqi.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val captured: Piece? = null
) {
    fun toNotation(): String {
        return "${piece.displayChar}(${from.col},${from.row})→(${to.col},${to.row})"
    }
}
