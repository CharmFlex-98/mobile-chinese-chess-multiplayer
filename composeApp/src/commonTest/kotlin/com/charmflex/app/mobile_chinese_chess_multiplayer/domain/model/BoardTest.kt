package com.charmflex.xiangqi.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BoardTest {

    @Test
    fun initialBoardHasCorrectPieceCount() {
        val board = Board.initial()
        val redPieces = board.getAllPieces(PieceColor.RED)
        val blackPieces = board.getAllPieces(PieceColor.BLACK)
        assertEquals(16, redPieces.size)
        assertEquals(16, blackPieces.size)
    }

    @Test
    fun initialBoardHasGeneralsInCorrectPositions() {
        val board = Board.initial()
        val redGeneral = board.findGeneral(PieceColor.RED)
        val blackGeneral = board.findGeneral(PieceColor.BLACK)
        assertEquals(Position(9, 4), redGeneral)
        assertEquals(Position(0, 4), blackGeneral)
    }

    @Test
    fun applyMoveMovesThePiece() {
        val board = Board.initial()
        val from = Position(9, 0) // red chariot
        val to = Position(8, 0)
        val piece = board[from]!!
        val move = Move(from, to, piece)
        val newBoard = board.applyMove(move)
        assertNull(newBoard[from])
        assertNotNull(newBoard[to])
        assertEquals(PieceType.CHARIOT, newBoard[to]!!.type)
    }

    @Test
    fun countPiecesBetweenOnInitialBoard() {
        val board = Board.initial()
        // Between red and black generals on col 4, there should be pieces in between
        val count = board.countPiecesBetween(Position(0, 4), Position(9, 4))
        // Soldiers at (3,4) and (6,4)
        assertEquals(2, count)
    }

    @Test
    fun emptyBoardHasNoPieces() {
        val board = Board.empty()
        assertEquals(0, board.getAllPieces(PieceColor.RED).size)
        assertEquals(0, board.getAllPieces(PieceColor.BLACK).size)
    }

    @Test
    fun setPieceWorks() {
        val board = Board.empty()
        val piece = Piece(PieceType.GENERAL, PieceColor.RED)
        val pos = Position(9, 4)
        val newBoard = board.setPiece(pos, piece)
        assertEquals(piece, newBoard[pos])
        assertNull(board[pos]) // original unchanged
    }
}
