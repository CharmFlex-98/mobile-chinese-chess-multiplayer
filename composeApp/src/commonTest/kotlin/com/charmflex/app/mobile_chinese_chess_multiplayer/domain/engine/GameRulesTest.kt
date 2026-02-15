package com.charmflex.xiangqi.engine.rules

import com.charmflex.xiangqi.engine.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameRulesTest {

    @Test
    fun initialBoardIsNotCheckmate() {
        val board = Board.initial()
        assertFalse(GameRules.isCheckmate(board, PieceColor.RED))
        assertFalse(GameRules.isCheckmate(board, PieceColor.BLACK))
    }

    @Test
    fun initialBoardIsNotStalemate() {
        val board = Board.initial()
        assertFalse(GameRules.isStalemate(board, PieceColor.RED))
        assertFalse(GameRules.isStalemate(board, PieceColor.BLACK))
    }

    @Test
    fun initialBoardStatusIsPlaying() {
        val board = Board.initial()
        assertEquals(GameStatus.PLAYING, GameRules.getGameStatus(board, PieceColor.RED))
    }

    @Test
    fun checkmateDetection() {
        // Set up a simple checkmate position:
        // Black general at (0,4), Red chariot at (0,0) giving check on row 0,
        // Red chariot at (1,0) covering row 1, Red general at (9,4)
        // Black general cannot escape
        val board = Board.empty()
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(0, 0), Piece(PieceType.CHARIOT, PieceColor.RED)) // check on row 0
            .setPiece(Position(1, 0), Piece(PieceType.CHARIOT, PieceColor.RED)) // covers row 1
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))

        assertTrue(MoveValidator.isInCheck(board, PieceColor.BLACK))
        // Black general is at (0,4). Red chariot at (0,0) attacks along row 0.
        // Black general can try: (1,4), (0,3), (0,5) but (1,0) chariot covers row 1 at (1,4)
        // (0,3) and (0,5) are still on row 0, attacked by chariot at (0,0)
        // Actually (0,3) and (0,5) - the chariot at (0,0) attacks the whole row,
        // but general at (0,4) blocks it from reaching (0,5) (chariot is blocked by general)
        // Let me reconsider: chariot at (0,0) slides right, hits general at (0,4).
        // (0,3) is between chariot and general, so it's attacked. (0,5) is beyond general.
        // Actually the general IS at (0,4), so chariot attacks (0,1),(0,2),(0,3),(0,4).
        // (0,5) is NOT attacked by this chariot since the general blocks it.
        // So general can go to (0,5) if not attacked by row 1 chariot.
        // Row 1 chariot at (1,0) attacks (1,1)...(1,8) and (0,0) is its column.
        // (0,5) is not attacked by (1,0) chariot. (1,4) IS attacked.
        // Need to check if (0,5) has flying general issue with (9,4) red general - same col? No, col 5 vs col 4.
        // So black can escape to (0,5). This is NOT checkmate.
        // Let me fix the position.

        // Better checkmate: two rooks on adjacent rows covering the palace
        val board2 = Board.empty()
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(0, 0), Piece(PieceType.CHARIOT, PieceColor.RED))
            .setPiece(Position(1, 8), Piece(PieceType.CHARIOT, PieceColor.RED))
            .setPiece(Position(9, 3), Piece(PieceType.GENERAL, PieceColor.RED)) // not on same col

        // Black general at (0,4) is attacked by chariot at (0,0) along row 0
        // Can go to (0,3)? Attacked by (0,0) chariot.
        // Can go to (0,5)? Not attacked by (0,0) (blocked by general at (0,4) before move, but after moving, chariot has clear path to (0,5)? No - after general moves from (0,4) to (0,5), the row 0 is: chariot at (0,0), empty (0,4), general at (0,5). Chariot can reach (0,5). So yes, attacked.
        // Wait, chariot at (0,0) attacks (0,1),(0,2),(0,3),(0,4),(0,5)...all the way, since after general moves away from (0,4), path is clear.
        // But we evaluate attacks on the NEW board position. So if general moves to (0,5), chariot at (0,0) has clear path to (0,5). Attacked!
        // Can go to (1,4)? Attacked by chariot at (1,8).
        // Can go to (1,3)? Attacked by chariot at (1,8)? No, chariot at (1,8) slides left: (1,7),(1,6),...(1,3). Yes attacked.
        // Can go to (1,5)? Attacked by chariot at (1,8). Yes.
        // All moves blocked! But wait - need to check flying general with (9,3).
        // If general goes to (1,4), is col 4 facing (9,3)? No, different cols. OK.
        // But (1,4) is attacked by (1,8) chariot, so still illegal.
        // Also need red general not facing - (9,3) col 3, black general candidate moves don't include col 3 palace.
        // So this should be checkmate.
        assertTrue(GameRules.isCheckmate(board2, PieceColor.BLACK))
        assertEquals(GameStatus.RED_WINS, GameRules.getGameStatus(board2, PieceColor.BLACK))
    }

    @Test
    fun notInCheckOnInitialBoard() {
        val board = Board.initial()
        assertFalse(GameRules.isInCheck(board, PieceColor.RED))
        assertFalse(GameRules.isInCheck(board, PieceColor.BLACK))
    }

    @Test
    fun flyingGeneralsFacingIsIllegal() {
        // Two generals facing each other on same column with no pieces between
        val board = Board.empty()
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
        assertTrue(MoveValidator.hasGeneralsFacing(board))
    }

    @Test
    fun flyingGeneralsNotFacingWithPieceBetween() {
        val board = Board.empty()
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(5, 4), Piece(PieceType.SOLDIER, PieceColor.RED))
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
        assertFalse(MoveValidator.hasGeneralsFacing(board))
    }

    @Test
    fun moveGeneratorProducesLegalMovesFromInitialBoard() {
        val board = Board.initial()
        val moves = MoveGenerator.generateLegalMoves(board, PieceColor.RED)
        assertTrue(moves.isNotEmpty())
        // All moves should leave the king safe
        for (move in moves) {
            val newBoard = board.applyMove(move)
            assertFalse(MoveValidator.isInCheck(newBoard, PieceColor.RED))
            assertFalse(MoveValidator.hasGeneralsFacing(newBoard))
        }
    }
}
