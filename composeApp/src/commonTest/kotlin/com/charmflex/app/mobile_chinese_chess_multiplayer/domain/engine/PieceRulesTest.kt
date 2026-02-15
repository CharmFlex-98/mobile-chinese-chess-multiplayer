package com.charmflex.xiangqi.engine.rules

import com.charmflex.xiangqi.engine.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PieceRulesTest {

    @Test
    fun generalMovesWithinPalace() {
        val board = Board.empty()
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.GENERAL, PieceColor.RED), Position(9, 4), board)
        // Can move up, left, right (all within palace). Down is off board.
        assertTrue(Position(8, 4) in moves)
        assertTrue(Position(9, 3) in moves)
        assertTrue(Position(9, 5) in moves)
        assertFalse(Position(9, 2) in moves) // outside palace
    }

    @Test
    fun generalCannotLeavePalace() {
        val board = Board.empty()
            .setPiece(Position(7, 3), Piece(PieceType.GENERAL, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.GENERAL, PieceColor.RED), Position(7, 3), board)
        for (pos in moves) {
            assertTrue(pos.isInPalace(PieceColor.RED), "General move $pos should be in palace")
        }
    }

    @Test
    fun advisorMovesDiagonallyInPalace() {
        val board = Board.empty()
            .setPiece(Position(9, 4), Piece(PieceType.ADVISOR, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.ADVISOR, PieceColor.RED), Position(9, 4), board)
        // From (9,4): only (8,3) and (8,5) are in palace
        assertTrue(Position(8, 3) in moves)
        assertTrue(Position(8, 5) in moves)
        assertEquals(2, moves.size)
    }

    @Test
    fun elephantMovesAndBlocking() {
        val board = Board.empty()
            .setPiece(Position(9, 2), Piece(PieceType.ELEPHANT, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.ELEPHANT, PieceColor.RED), Position(9, 2), board)
        assertTrue(Position(7, 0) in moves)
        assertTrue(Position(7, 4) in moves)
        // Cannot cross river, so (5,0) and (5,4) should be the farthest
        for (pos in moves) {
            assertTrue(pos.isOnSide(PieceColor.RED), "Elephant should stay on own side: $pos")
        }
    }

    @Test
    fun elephantBlockedByInterveningPiece() {
        val board = Board.empty()
            .setPiece(Position(9, 2), Piece(PieceType.ELEPHANT, PieceColor.RED))
            .setPiece(Position(8, 1), Piece(PieceType.SOLDIER, PieceColor.RED)) // blocks top-left
        val moves = PieceRules.getMoves(Piece(PieceType.ELEPHANT, PieceColor.RED), Position(9, 2), board)
        assertFalse(Position(7, 0) in moves) // blocked
        assertTrue(Position(7, 4) in moves) // not blocked
    }

    @Test
    fun horseMoves() {
        val board = Board.empty()
            .setPiece(Position(5, 4), Piece(PieceType.HORSE, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.HORSE, PieceColor.RED), Position(5, 4), board)
        // Center horse should have 8 moves
        assertEquals(8, moves.size)
        assertTrue(Position(3, 3) in moves)
        assertTrue(Position(3, 5) in moves)
        assertTrue(Position(4, 2) in moves)
        assertTrue(Position(4, 6) in moves)
    }

    @Test
    fun horseBlockedByLeg() {
        val board = Board.empty()
            .setPiece(Position(5, 4), Piece(PieceType.HORSE, PieceColor.RED))
            .setPiece(Position(4, 4), Piece(PieceType.SOLDIER, PieceColor.RED)) // blocks upward
        val moves = PieceRules.getMoves(Piece(PieceType.HORSE, PieceColor.RED), Position(5, 4), board)
        assertFalse(Position(3, 3) in moves) // blocked by leg at (4,4)
        assertFalse(Position(3, 5) in moves) // blocked by leg at (4,4)
    }

    @Test
    fun chariotMovesOnEmptyBoard() {
        val board = Board.empty()
            .setPiece(Position(5, 4), Piece(PieceType.CHARIOT, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.CHARIOT, PieceColor.RED), Position(5, 4), board)
        // Should be able to reach any square on row 5 and col 4
        assertEquals(17, moves.size) // 9 horizontal + 8 vertical (row 0-4,6-9 + col 0-3,5-8)
    }

    @Test
    fun chariotBlockedByPiece() {
        val board = Board.empty()
            .setPiece(Position(5, 4), Piece(PieceType.CHARIOT, PieceColor.RED))
            .setPiece(Position(5, 6), Piece(PieceType.SOLDIER, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.CHARIOT, PieceColor.RED), Position(5, 4), board)
        assertFalse(Position(5, 6) in moves) // blocked by own piece
        assertFalse(Position(5, 7) in moves) // behind own piece
    }

    @Test
    fun cannonMovesAndCaptures() {
        val board = Board.empty()
            .setPiece(Position(5, 4), Piece(PieceType.CANNON, PieceColor.RED))
            .setPiece(Position(5, 6), Piece(PieceType.SOLDIER, PieceColor.RED)) // screen piece
            .setPiece(Position(5, 8), Piece(PieceType.SOLDIER, PieceColor.BLACK)) // target behind screen
        val moves = PieceRules.getMoves(Piece(PieceType.CANNON, PieceColor.RED), Position(5, 4), board)
        // Can move to (5,5) but not (5,6) - screen piece
        assertTrue(Position(5, 5) in moves)
        assertFalse(Position(5, 6) in moves)
        // Can capture (5,8) - jumps over screen at (5,6)
        assertTrue(Position(5, 8) in moves)
        assertFalse(Position(5, 7) in moves) // between screen and target
    }

    @Test
    fun soldierMovesBeforeRiver() {
        val board = Board.empty()
            .setPiece(Position(6, 4), Piece(PieceType.SOLDIER, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.SOLDIER, PieceColor.RED), Position(6, 4), board)
        // Before crossing river, only forward (for red, row decreases)
        assertEquals(1, moves.size)
        assertTrue(Position(5, 4) in moves)
    }

    @Test
    fun soldierMovesAfterRiver() {
        val board = Board.empty()
            .setPiece(Position(4, 4), Piece(PieceType.SOLDIER, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.SOLDIER, PieceColor.RED), Position(4, 4), board)
        // After crossing river: forward, left, right
        assertEquals(3, moves.size)
        assertTrue(Position(3, 4) in moves) // forward
        assertTrue(Position(4, 3) in moves) // left
        assertTrue(Position(4, 5) in moves) // right
    }

    @Test
    fun flyingGeneralRule() {
        val board = Board.empty()
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
        val moves = PieceRules.getMoves(Piece(PieceType.GENERAL, PieceColor.RED), Position(9, 4), board)
        // Red general should be able to "capture" black general via flying general rule
        assertTrue(Position(0, 4) in moves)
    }
}
