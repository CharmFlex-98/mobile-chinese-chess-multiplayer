package com.charmflex.xiangqi.engine.rules

import com.charmflex.xiangqi.engine.model.*

object MoveValidator {

    fun isLegal(move: Move, board: Board): Boolean {
        val newBoard = board.applyMove(move)
        if (isInCheck(newBoard, move.piece.color)) return false
        if (hasGeneralsFacing(newBoard)) return false
        return true
    }

    fun isInCheck(board: Board, color: PieceColor): Boolean {
        val generalPos = board.findGeneral(color) ?: return true
        val opponentColor = color.opponent
        for ((pos, piece) in board.getAllPieces(opponentColor)) {
            val moves = PieceRules.getMoves(piece, pos, board)
            if (generalPos in moves) return true
        }
        return false
    }

    fun hasGeneralsFacing(board: Board): Boolean {
        val redGeneral = board.findGeneral(PieceColor.RED) ?: return false
        val blackGeneral = board.findGeneral(PieceColor.BLACK) ?: return false
        if (redGeneral.col != blackGeneral.col) return false
        return board.countPiecesBetween(redGeneral, blackGeneral) == 0
    }
}
