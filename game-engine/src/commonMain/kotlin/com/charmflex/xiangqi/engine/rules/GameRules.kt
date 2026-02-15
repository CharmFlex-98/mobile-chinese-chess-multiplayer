package com.charmflex.xiangqi.engine.rules

import com.charmflex.xiangqi.engine.model.*

object GameRules {

    fun isCheckmate(board: Board, color: PieceColor): Boolean {
        if (!MoveValidator.isInCheck(board, color)) return false
        return MoveGenerator.generateLegalMoves(board, color).isEmpty()
    }

    fun isStalemate(board: Board, color: PieceColor): Boolean {
        if (MoveValidator.isInCheck(board, color)) return false
        return MoveGenerator.generateLegalMoves(board, color).isEmpty()
    }

    fun getGameStatus(board: Board, currentTurn: PieceColor): GameStatus {
        if (isCheckmate(board, currentTurn)) {
            return if (currentTurn == PieceColor.RED) GameStatus.BLACK_WINS else GameStatus.RED_WINS
        }
        if (isStalemate(board, currentTurn)) {
            return if (currentTurn == PieceColor.RED) GameStatus.BLACK_WINS else GameStatus.RED_WINS
        }
        return GameStatus.PLAYING
    }

    fun isInCheck(board: Board, color: PieceColor): Boolean {
        return MoveValidator.isInCheck(board, color)
    }
}
