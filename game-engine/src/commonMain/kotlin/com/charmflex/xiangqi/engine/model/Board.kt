package com.charmflex.xiangqi.engine.model

class Board private constructor(
    private val grid: Array<Array<Piece?>>
) {
    companion object {
        const val ROWS = 10
        const val COLS = 9

        fun initial(): Board {
            val grid = Array(ROWS) { arrayOfNulls<Piece>(COLS) }

            // Black pieces (top, rows 0-4)
            grid[0][0] = Piece(PieceType.CHARIOT, PieceColor.BLACK)
            grid[0][1] = Piece(PieceType.HORSE, PieceColor.BLACK)
            grid[0][2] = Piece(PieceType.ELEPHANT, PieceColor.BLACK)
            grid[0][3] = Piece(PieceType.ADVISOR, PieceColor.BLACK)
            grid[0][4] = Piece(PieceType.GENERAL, PieceColor.BLACK)
            grid[0][5] = Piece(PieceType.ADVISOR, PieceColor.BLACK)
            grid[0][6] = Piece(PieceType.ELEPHANT, PieceColor.BLACK)
            grid[0][7] = Piece(PieceType.HORSE, PieceColor.BLACK)
            grid[0][8] = Piece(PieceType.CHARIOT, PieceColor.BLACK)
            grid[2][1] = Piece(PieceType.CANNON, PieceColor.BLACK)
            grid[2][7] = Piece(PieceType.CANNON, PieceColor.BLACK)
            grid[3][0] = Piece(PieceType.SOLDIER, PieceColor.BLACK)
            grid[3][2] = Piece(PieceType.SOLDIER, PieceColor.BLACK)
            grid[3][4] = Piece(PieceType.SOLDIER, PieceColor.BLACK)
            grid[3][6] = Piece(PieceType.SOLDIER, PieceColor.BLACK)
            grid[3][8] = Piece(PieceType.SOLDIER, PieceColor.BLACK)

            // Red pieces (bottom, rows 5-9)
            grid[9][0] = Piece(PieceType.CHARIOT, PieceColor.RED)
            grid[9][1] = Piece(PieceType.HORSE, PieceColor.RED)
            grid[9][2] = Piece(PieceType.ELEPHANT, PieceColor.RED)
            grid[9][3] = Piece(PieceType.ADVISOR, PieceColor.RED)
            grid[9][4] = Piece(PieceType.GENERAL, PieceColor.RED)
            grid[9][5] = Piece(PieceType.ADVISOR, PieceColor.RED)
            grid[9][6] = Piece(PieceType.ELEPHANT, PieceColor.RED)
            grid[9][7] = Piece(PieceType.HORSE, PieceColor.RED)
            grid[9][8] = Piece(PieceType.CHARIOT, PieceColor.RED)
            grid[7][1] = Piece(PieceType.CANNON, PieceColor.RED)
            grid[7][7] = Piece(PieceType.CANNON, PieceColor.RED)
            grid[6][0] = Piece(PieceType.SOLDIER, PieceColor.RED)
            grid[6][2] = Piece(PieceType.SOLDIER, PieceColor.RED)
            grid[6][4] = Piece(PieceType.SOLDIER, PieceColor.RED)
            grid[6][6] = Piece(PieceType.SOLDIER, PieceColor.RED)
            grid[6][8] = Piece(PieceType.SOLDIER, PieceColor.RED)

            return Board(grid)
        }

        fun empty(): Board = Board(Array(ROWS) { arrayOfNulls(COLS) })
    }

    operator fun get(pos: Position): Piece? = grid[pos.row][pos.col]

    operator fun get(row: Int, col: Int): Piece? = grid[row][col]

    fun setPiece(pos: Position, piece: Piece?): Board {
        val newGrid = grid.map { it.copyOf() }.toTypedArray()
        newGrid[pos.row][pos.col] = piece
        return Board(newGrid)
    }

    fun applyMove(move: Move): Board {
        val newGrid = grid.map { it.copyOf() }.toTypedArray()
        newGrid[move.to.row][move.to.col] = newGrid[move.from.row][move.from.col]
        newGrid[move.from.row][move.from.col] = null
        return Board(newGrid)
    }

    fun findGeneral(color: PieceColor): Position? {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val piece = grid[row][col]
                if (piece != null && piece.type == PieceType.GENERAL && piece.color == color) {
                    return Position(row, col)
                }
            }
        }
        return null
    }

    fun getAllPieces(color: PieceColor): List<Pair<Position, Piece>> {
        val pieces = mutableListOf<Pair<Position, Piece>>()
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val piece = grid[row][col]
                if (piece != null && piece.color == color) {
                    pieces.add(Position(row, col) to piece)
                }
            }
        }
        return pieces
    }

    fun countPiecesBetween(from: Position, to: Position): Int {
        var count = 0
        if (from.row == to.row) {
            val minCol = minOf(from.col, to.col) + 1
            val maxCol = maxOf(from.col, to.col)
            for (col in minCol until maxCol) {
                if (grid[from.row][col] != null) count++
            }
        } else if (from.col == to.col) {
            val minRow = minOf(from.row, to.row) + 1
            val maxRow = maxOf(from.row, to.row)
            for (row in minRow until maxRow) {
                if (grid[row][from.col] != null) count++
            }
        }
        return count
    }

    fun copy(): Board = Board(grid.map { it.copyOf() }.toTypedArray())

    fun makeMove(move: Move) {
        grid[move.to.row][move.to.col] = grid[move.from.row][move.from.col]
        grid[move.from.row][move.from.col] = null
    }

    fun unmakeMove(move: Move) {
        grid[move.from.row][move.from.col] = move.piece
        grid[move.to.row][move.to.col] = move.captured
    }
}
