package com.charmflex.xiangqi.engine.ai

import com.charmflex.xiangqi.engine.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluatorTest {

    @Test
    fun initialBoardIsEqualForBothSides() {
        val board = Board.initial()
        val redScore = Evaluator.evaluate(board, PieceColor.RED)
        val blackScore = Evaluator.evaluate(board, PieceColor.BLACK)
        // Initial position should be symmetric, scores should be equal
        assertEquals(redScore, -blackScore)
    }

    @Test
    fun materialAdvantageScoresHigher() {
        // Board with extra chariot for red
        val board = Board.empty()
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(5, 0), Piece(PieceType.CHARIOT, PieceColor.RED))

        val redScore = Evaluator.evaluate(board, PieceColor.RED)
        val blackScore = Evaluator.evaluate(board, PieceColor.BLACK)
        assertTrue(redScore > 0, "Red with extra chariot should have positive score")
        assertTrue(blackScore < 0, "Black without extra piece should have negative score")
    }

    @Test
    fun emptyBoardWithOnlyGeneralsIsZero() {
        val board = Board.empty()
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
        val redScore = Evaluator.evaluate(board, PieceColor.RED)
        // Should be 0 since both sides have equal material
        assertEquals(0, redScore)
    }

    @Test
    fun soldierAfterRiverWorthMore() {
        // Soldier on own side (row 6 for red = before river)
        val boardBefore = Board.empty()
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(6, 4), Piece(PieceType.SOLDIER, PieceColor.RED))

        // Soldier across river (row 3 for red = after river)
        val boardAfter = Board.empty()
            .setPiece(Position(9, 4), Piece(PieceType.GENERAL, PieceColor.RED))
            .setPiece(Position(0, 4), Piece(PieceType.GENERAL, PieceColor.BLACK))
            .setPiece(Position(3, 4), Piece(PieceType.SOLDIER, PieceColor.RED))

        val scoreBefore = Evaluator.evaluate(boardBefore, PieceColor.RED)
        val scoreAfter = Evaluator.evaluate(boardAfter, PieceColor.RED)
        assertTrue(scoreAfter > scoreBefore, "Soldier after river should score higher")
    }
}
