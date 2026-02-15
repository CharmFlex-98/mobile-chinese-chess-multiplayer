package com.charmflex.xiangqi.engine.rules

import com.charmflex.xiangqi.engine.model.*

object MoveGenerator {

    fun generateMoves(board: Board, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        for ((pos, piece) in board.getAllPieces(color)) {
            val targets = PieceRules.getMoves(piece, pos, board)
            for (target in targets) {
                moves.add(Move(from = pos, to = target, piece = piece, captured = board[target]))
            }
        }
        return moves
    }

    fun generateLegalMoves(board: Board, color: PieceColor): List<Move> {
        return generateMoves(board, color).filter { move ->
            MoveValidator.isLegal(move, board)
        }
    }

    fun generateCaptureMoves(board: Board, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        for ((pos, piece) in board.getAllPieces(color)) {
            val targets = PieceRules.getMoves(piece, pos, board)
            for (target in targets) {
                val captured = board[target]
                if (captured != null) {
                    moves.add(Move(from = pos, to = target, piece = piece, captured = captured))
                }
            }
        }
        return moves
    }

    fun generateLegalMovesFrom(board: Board, pos: Position): List<Move> {
        val piece = board[pos] ?: return emptyList()
        val targets = PieceRules.getMoves(piece, pos, board)
        return targets.map { target ->
            Move(from = pos, to = target, piece = piece, captured = board[target])
        }.filter { move ->
            MoveValidator.isLegal(move, board)
        }
    }
}
