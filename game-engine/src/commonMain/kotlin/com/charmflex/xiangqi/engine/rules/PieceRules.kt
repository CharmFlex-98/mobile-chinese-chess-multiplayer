package com.charmflex.xiangqi.engine.rules

import com.charmflex.xiangqi.engine.model.*

object PieceRules {

    fun getMoves(piece: Piece, pos: Position, board: Board): List<Position> {
        return when (piece.type) {
            PieceType.GENERAL -> getGeneralMoves(pos, piece.color, board)
            PieceType.ADVISOR -> getAdvisorMoves(pos, piece.color, board)
            PieceType.ELEPHANT -> getElephantMoves(pos, piece.color, board)
            PieceType.HORSE -> getHorseMoves(pos, piece.color, board)
            PieceType.CHARIOT -> getChariotMoves(pos, piece.color, board)
            PieceType.CANNON -> getCannonMoves(pos, piece.color, board)
            PieceType.SOLDIER -> getSoldierMoves(pos, piece.color, board)
        }
    }

    private fun getGeneralMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val deltas = listOf(Position(-1, 0), Position(1, 0), Position(0, -1), Position(0, 1))
        for (d in deltas) {
            val target = pos + d
            if (target.isValid() && target.isInPalace(color)) {
                val targetPiece = board[target]
                if (targetPiece == null || targetPiece.color != color) {
                    moves.add(target)
                }
            }
        }
        // Flying general: can capture opposing general on same column with no pieces between
        val opponentGeneralPos = board.findGeneral(color.opponent)
        if (opponentGeneralPos != null && opponentGeneralPos.col == pos.col) {
            if (board.countPiecesBetween(pos, opponentGeneralPos) == 0) {
                moves.add(opponentGeneralPos)
            }
        }
        return moves
    }

    private fun getAdvisorMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val deltas = listOf(Position(-1, -1), Position(-1, 1), Position(1, -1), Position(1, 1))
        for (d in deltas) {
            val target = pos + d
            if (target.isValid() && target.isInPalace(color)) {
                val targetPiece = board[target]
                if (targetPiece == null || targetPiece.color != color) {
                    moves.add(target)
                }
            }
        }
        return moves
    }

    private fun getElephantMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val deltas = listOf(
            Position(-2, -2) to Position(-1, -1),
            Position(-2, 2) to Position(-1, 1),
            Position(2, -2) to Position(1, -1),
            Position(2, 2) to Position(1, 1)
        )
        for ((delta, blocking) in deltas) {
            val target = pos + delta
            val blockPos = pos + blocking
            if (target.isValid() && target.isOnSide(color) && board[blockPos] == null) {
                val targetPiece = board[target]
                if (targetPiece == null || targetPiece.color != color) {
                    moves.add(target)
                }
            }
        }
        return moves
    }

    private fun getHorseMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val deltas = listOf(
            Position(-2, -1) to Position(-1, 0),
            Position(-2, 1) to Position(-1, 0),
            Position(2, -1) to Position(1, 0),
            Position(2, 1) to Position(1, 0),
            Position(-1, -2) to Position(0, -1),
            Position(-1, 2) to Position(0, 1),
            Position(1, -2) to Position(0, -1),
            Position(1, 2) to Position(0, 1)
        )
        for ((delta, leg) in deltas) {
            val target = pos + delta
            val legPos = pos + leg
            if (target.isValid() && board[legPos] == null) {
                val targetPiece = board[target]
                if (targetPiece == null || targetPiece.color != color) {
                    moves.add(target)
                }
            }
        }
        return moves
    }

    private fun getChariotMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        return getSlidingMoves(pos, color, board)
    }

    private fun getCannonMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val directions = listOf(Position(0, 1), Position(0, -1), Position(1, 0), Position(-1, 0))
        for (dir in directions) {
            var current = pos + dir
            var jumped = false
            while (current.isValid()) {
                val piece = board[current]
                if (!jumped) {
                    if (piece == null) {
                        moves.add(current)
                    } else {
                        jumped = true
                    }
                } else {
                    if (piece != null) {
                        if (piece.color != color) {
                            moves.add(current)
                        }
                        break
                    }
                }
                current = current + dir
            }
        }
        return moves
    }

    private fun getSoldierMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val forward = if (color == PieceColor.RED) Position(-1, 0) else Position(1, 0)

        val forwardPos = pos + forward
        if (forwardPos.isValid()) {
            val piece = board[forwardPos]
            if (piece == null || piece.color != color) {
                moves.add(forwardPos)
            }
        }

        if (pos.hasPassedRiver(color)) {
            val sideways = listOf(Position(0, -1), Position(0, 1))
            for (dir in sideways) {
                val target = pos + dir
                if (target.isValid()) {
                    val piece = board[target]
                    if (piece == null || piece.color != color) {
                        moves.add(target)
                    }
                }
            }
        }

        return moves
    }

    private fun getSlidingMoves(pos: Position, color: PieceColor, board: Board): List<Position> {
        val moves = mutableListOf<Position>()
        val directions = listOf(Position(0, 1), Position(0, -1), Position(1, 0), Position(-1, 0))
        for (dir in directions) {
            var current = pos + dir
            while (current.isValid()) {
                val piece = board[current]
                if (piece == null) {
                    moves.add(current)
                } else {
                    if (piece.color != color) {
                        moves.add(current)
                    }
                    break
                }
                current = current + dir
            }
        }
        return moves
    }
}
