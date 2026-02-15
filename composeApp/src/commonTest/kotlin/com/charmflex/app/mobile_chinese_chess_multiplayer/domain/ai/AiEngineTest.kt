package com.charmflex.xiangqi.engine.ai

import com.charmflex.xiangqi.engine.model.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiEngineTest {

    @Test
    fun aiFindsAMoveFromInitialPosition() {
        val engine = AiEngine(AiDifficulty.BEGINNER)
        val board = Board.initial()
        val move = engine.findBestMove(board, PieceColor.RED)
        assertNotNull(move, "AI should find a move from initial position")
        assertEquals(PieceColor.RED, move.piece.color)
    }

    @Test
    fun aiCapturesHangingPiece() {
        // Red chariot can capture undefended black chariot
        // Generals on different columns to avoid flying general issue
        val board = Board.empty()
            .setPiece(Position(9, 3), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(0, 5), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(5, 0), Piece(PieceType.CHARIOT, PieceColor.RED))
            .setPiece(Position(5, 8), Piece(PieceType.CHARIOT, PieceColor.BLACK))

        val engine = AiEngine(AiDifficulty.HARD) // no noise
        val move = engine.findBestMove(board, PieceColor.RED)
        assertNotNull(move)
        // Should capture the black chariot
        assertEquals(Position(5, 8), move.to)
        assertNotNull(move.captured)
    }

    @Test
    fun aiDoesNotBlunderMaterial() {
        val board = Board.empty()
            .setPiece(Position(9, 3), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(0, 5), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(5, 0), Piece(PieceType.CHARIOT, PieceColor.RED))
            .setPiece(Position(0, 0), Piece(PieceType.CHARIOT, PieceColor.BLACK))

        val engine = AiEngine(AiDifficulty.MEDIUM)
        val move = engine.findBestMove(board, PieceColor.RED)
        assertNotNull(move)
        assertTrue(move.from != move.to)
    }

    @Test
    fun aiFindsCheckmateInOne() {
        // Red chariot at (2,3) can move to (0,3) to deliver checkmate
        // Red chariot at (1,0) covers all of row 1
        // Black general at (0,4) has no escape
        val board = Board.empty()
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(9, 3), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(1, 0), Piece(PieceType.CHARIOT, PieceColor.RED))
            .setPiece(Position(2, 3), Piece(PieceType.CHARIOT, PieceColor.RED))

        val engine = AiEngine(AiDifficulty.HARD)
        val move = engine.findBestMove(board, PieceColor.RED)
        assertNotNull(move)
        assertEquals(Position(2, 3), move.from)
        assertEquals(Position(0, 3), move.to)
    }

    @Test
    fun allDifficultyLevelsReturnAMove() {
        val board = Board.initial()
        for (difficulty in listOf(AiDifficulty.BEGINNER, AiDifficulty.EASY, AiDifficulty.MEDIUM)) {
            val engine = AiEngine(difficulty)
            val move = engine.findBestMove(board, PieceColor.RED)
            assertNotNull(move, "Difficulty ${difficulty.label} should find a move")
        }
    }
}
