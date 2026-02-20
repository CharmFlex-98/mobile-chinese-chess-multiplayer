package com.charmflex.xiangqi.engine.ai

import com.charmflex.xiangqi.engine.rules.MoveGenerator
import com.charmflex.xiangqi.engine.rules.MoveValidator
import com.charmflex.xiangqi.engine.model.*
import kotlin.random.Random
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class AiEngine(private val difficulty: AiDifficulty) {

    companion object {
        private const val CHECKMATE_SCORE = 100_000
        private const val TT_SIZE = 1 shl 18
        private const val MAX_PLY = 64
        private const val NULL_MOVE_R = 2
        private const val LMR_FULL_DEPTH_MOVES = 4
        private const val LMR_REDUCTION_LIMIT = 3
        private const val DELTA_MARGIN = 200
        private const val MAX_QUIESCENCE_DEPTH = 8
        private const val ASPIRATION_WINDOW = 50
        private const val MAX_EXTENSIONS = 4
        private const val INF = Int.MAX_VALUE / 2
        private const val TIME_CHECK_INTERVAL = 4096L
        // Sentinel for "no move". Safe because from==to is never a legal move, so 0 is never
        // produced by encodeMove() for any real position.
        private const val NO_MOVE = 0
    }

    // Transposition table stored as parallel primitive arrays instead of an object array.
    // This eliminates per-entry object allocation and GC pressure for 262,144 entries.
    // Move is encoded as: from.row | (from.col shl 4) | (to.row shl 8) | (to.col shl 12).
    private val ttHashes = LongArray(TT_SIZE)
    private val ttDepths = IntArray(TT_SIZE)
    private val ttScores = IntArray(TT_SIZE)
    private val ttFlags  = IntArray(TT_SIZE)
    private val ttMoves  = IntArray(TT_SIZE)

    private var nodesSearched = 0L

    // Killer moves encoded as Int (same format as ttMoves) — avoids Move object allocations.
    private val killerMoves = Array(MAX_PLY) { IntArray(2) }

    private val historyTable = Array(2) { Array(10) { Array(9) { Array(10) { IntArray(9) } } } }

    private var searchStart: TimeMark? = null
    private var searchAborted = false

    /** Encodes only the from/to squares of a move into a single Int. */
    private fun encodeMove(move: Move): Int =
        move.from.row or (move.from.col shl 4) or (move.to.row shl 8) or (move.to.col shl 12)

    /** Returns true if [move] has the same from/to squares as [encoded]. */
    private fun sameMove(move: Move, encoded: Int): Boolean {
        if (encoded == NO_MOVE) return false
        return move.from.row == (encoded and 0xF) &&
               move.from.col == ((encoded shr 4) and 0xF) &&
               move.to.row   == ((encoded shr 8) and 0xF) &&
               move.to.col   == ((encoded shr 12) and 0xF)
    }

    fun findBestMove(board: Board, color: PieceColor): Move? {
        nodesSearched = 0
        searchAborted = false
        searchStart = null
        clearKillers()
        clearHistory()

        val depth = difficulty.depth
        val searchBoard = board.copy()
        var bestMove: Move? = null
        var previousScore = 0

        for (d in 1..depth) {
            val result: Pair<Move?, Int>

            if (d <= 2) {
                result = searchRoot(searchBoard, color, d, -INF, INF)
            } else {
                val alpha = previousScore - ASPIRATION_WINDOW
                val beta = previousScore + ASPIRATION_WINDOW
                result = searchRoot(searchBoard, color, d, alpha, beta)

                if (!searchAborted && (result.second <= alpha || result.second >= beta)) {
                    val fullResult = searchRoot(searchBoard, color, d, -INF, INF)
                    if (!searchAborted && fullResult.first != null) {
                        bestMove = fullResult.first
                        previousScore = fullResult.second
                        continue
                    }
                }
            }

            if (searchAborted) break

            if (result.first != null) {
                bestMove = result.first
                previousScore = result.second
            }

            if (d == difficulty.minDepth && difficulty.timeLimitMs > 0 && searchStart == null) {
                searchStart = TimeSource.Monotonic.markNow()
            }
        }

        return bestMove
    }

    private fun checkTime() {
        val start = searchStart ?: return
        if (nodesSearched and (TIME_CHECK_INTERVAL - 1) != 0L) return
        if (start.elapsedNow().inWholeMilliseconds >= difficulty.timeLimitMs) {
            searchAborted = true
        }
    }

    private fun clearKillers() {
        for (ply in killerMoves.indices) {
            killerMoves[ply][0] = NO_MOVE
            killerMoves[ply][1] = NO_MOVE
        }
    }

    private fun clearHistory() {
        for (c in historyTable.indices)
            for (fr in historyTable[c].indices)
                for (fc in historyTable[c][fr].indices)
                    for (tr in historyTable[c][fr][fc].indices)
                        historyTable[c][fr][fc][tr].fill(0)
    }

    private fun searchRoot(board: Board, color: PieceColor, depth: Int, alpha: Int, beta: Int): Pair<Move?, Int> {
        val moves = MoveGenerator.generateMoves(board, color)
        if (moves.isEmpty()) return null to -INF

        val hash = ZobristHash.hash(board, color)
        val ttIndex = (hash and (TT_SIZE - 1).toLong()).toInt()
        val ttBestMove = if (ttHashes[ttIndex] == hash) ttMoves[ttIndex] else NO_MOVE

        val orderedMoves = orderMovesForSearch(moves, ttBestMove, 0)

        var bestScore = -INF
        var bestMove: Move? = null
        var currentAlpha = alpha
        var legalMoveCount = 0

        for (move in orderedMoves) {
            if (searchAborted) break

            board.makeMove(move)

            if (MoveValidator.isInCheck(board, move.piece.color) || MoveValidator.hasGeneralsFacing(board)) {
                board.unmakeMove(move)
                continue
            }

            legalMoveCount++
            val newHash = updateHash(hash, move)

            val givesCheck = MoveValidator.isInCheck(board, color.opponent)
            val extension = if (givesCheck) 1 else 0

            val score: Int
            if (legalMoveCount == 1) {
                score = -alphaBeta(board, color.opponent, depth - 1 + extension, -beta, -currentAlpha, 1, newHash, true, givesCheck, extension)
            } else {
                var pvsScore = -alphaBeta(board, color.opponent, depth - 1 + extension, -currentAlpha - 1, -currentAlpha, 1, newHash, true, givesCheck, extension)
                if (!searchAborted && pvsScore > currentAlpha && pvsScore < beta) {
                    pvsScore = -alphaBeta(board, color.opponent, depth - 1 + extension, -beta, -currentAlpha, 1, newHash, true, givesCheck, extension)
                }
                score = pvsScore
            }

            board.unmakeMove(move)

            if (searchAborted) break

            val adjustedScore = if (difficulty.noiseRange > 0) {
                score + Random.nextInt(-difficulty.noiseRange, difficulty.noiseRange + 1)
            } else {
                score
            }

            if (adjustedScore > bestScore) {
                bestScore = adjustedScore
                bestMove = move
            }
            if (score > currentAlpha) {
                currentAlpha = score
            }
            if (score >= beta) break
        }

        return bestMove to bestScore
    }

    private fun alphaBeta(
        board: Board, color: PieceColor, depth: Int,
        alpha: Int, beta: Int, ply: Int, hash: Long,
        allowNullMove: Boolean, inCheck: Boolean, totalExtensions: Int
    ): Int {
        if (searchAborted) return 0

        nodesSearched++
        checkTime()
        if (searchAborted) return 0

        val ttIndex = (hash and (TT_SIZE - 1).toLong()).toInt()
        var ttBestMove = NO_MOVE
        if (ttHashes[ttIndex] == hash) {
            ttBestMove = ttMoves[ttIndex]
            if (ttDepths[ttIndex] >= depth) {
                when (ttFlags[ttIndex]) {
                    0 -> return ttScores[ttIndex]
                    1 -> if (ttScores[ttIndex] >= beta) return ttScores[ttIndex]
                    2 -> if (ttScores[ttIndex] <= alpha) return ttScores[ttIndex]
                }
            }
        }

        if (depth <= 0) {
            return quiescence(board, color, alpha, beta, hash, 0)
        }

        if (allowNullMove && !inCheck && depth >= NULL_MOVE_R + 1) {
            val nullHash = hash xor ZobristHash.sideHash()
            val nullScore = -alphaBeta(
                board, color.opponent, depth - 1 - NULL_MOVE_R,
                -beta, -beta + 1, ply + 1, nullHash, false,
                inCheck = false, totalExtensions = totalExtensions
            )
            if (nullScore >= beta) {
                return nullScore
            }
        }

        val moves = MoveGenerator.generateMoves(board, color)
        val orderedMoves = orderMovesForSearch(moves, ttBestMove, ply)

        var currentAlpha = alpha
        var bestMove = NO_MOVE
        var legalMoveCount = 0

        for (move in orderedMoves) {
            if (searchAborted) return 0

            board.makeMove(move)

            if (MoveValidator.isInCheck(board, move.piece.color) || MoveValidator.hasGeneralsFacing(board)) {
                board.unmakeMove(move)
                continue
            }

            legalMoveCount++
            val newHash = updateHash(hash, move)

            val givesCheck = if (totalExtensions < MAX_EXTENSIONS) {
                MoveValidator.isInCheck(board, color.opponent)
            } else {
                false
            }
            val extension = if (givesCheck) 1 else 0
            val childExtensions = totalExtensions + extension

            val score: Int

            if (legalMoveCount == 1) {
                score = -alphaBeta(board, color.opponent, depth - 1 + extension, -beta, -currentAlpha, ply + 1, newHash, true, givesCheck, childExtensions)
            } else {
                val reduction = if (legalMoveCount > LMR_FULL_DEPTH_MOVES
                    && depth >= LMR_REDUCTION_LIMIT
                    && move.captured == null
                    && !givesCheck
                    && !inCheck
                ) 1 else 0

                var pvsScore = -alphaBeta(
                    board, color.opponent, depth - 1 - reduction + extension,
                    -currentAlpha - 1, -currentAlpha, ply + 1, newHash, true, givesCheck, childExtensions
                )

                if (!searchAborted && pvsScore > currentAlpha && (pvsScore < beta || reduction > 0)) {
                    pvsScore = -alphaBeta(board, color.opponent, depth - 1 + extension, -beta, -currentAlpha, ply + 1, newHash, true, givesCheck, childExtensions)
                }

                score = pvsScore
            }

            board.unmakeMove(move)

            if (searchAborted) return 0

            if (score >= beta) {
                val encoded = encodeMove(move)
                ttHashes[ttIndex] = hash
                ttDepths[ttIndex] = depth
                ttScores[ttIndex] = score
                ttFlags[ttIndex]  = 1
                ttMoves[ttIndex]  = encoded

                if (move.captured == null && ply < MAX_PLY) {
                    if (killerMoves[ply][0] != encoded) {
                        killerMoves[ply][1] = killerMoves[ply][0]
                        killerMoves[ply][0] = encoded
                    }
                    val ci = if (color == PieceColor.RED) 0 else 1
                    historyTable[ci][move.from.row][move.from.col][move.to.row][move.to.col] += depth * depth
                }

                return score
            }
            if (score > currentAlpha) {
                currentAlpha = score
                bestMove = encodeMove(move)
            }
        }

        if (legalMoveCount == 0) {
            return if (inCheck) {
                -CHECKMATE_SCORE - depth
            } else {
                -CHECKMATE_SCORE - depth
            }
        }

        val flag = if (currentAlpha > alpha) 0 else 2
        ttHashes[ttIndex] = hash
        ttDepths[ttIndex] = depth
        ttScores[ttIndex] = currentAlpha
        ttFlags[ttIndex]  = flag
        ttMoves[ttIndex]  = bestMove

        return currentAlpha
    }

    private fun quiescence(board: Board, color: PieceColor, alpha: Int, beta: Int, hash: Long, qDepth: Int): Int {
        if (searchAborted) return 0

        val standPat = Evaluator.evaluate(board, color)
        if (standPat >= beta) return standPat
        if (qDepth >= MAX_QUIESCENCE_DEPTH) return standPat

        var currentAlpha = maxOf(alpha, standPat)

        val captures = MoveGenerator.generateCaptureMoves(board, color)
            .sortedByDescending { captureValue(it) }

        for (move in captures) {
            if (searchAborted) return 0

            val deltaScore = standPat + Evaluator.pieceValue(move.captured!!.type) + DELTA_MARGIN
            if (deltaScore < currentAlpha) continue

            board.makeMove(move)

            if (MoveValidator.isInCheck(board, move.piece.color) || MoveValidator.hasGeneralsFacing(board)) {
                board.unmakeMove(move)
                continue
            }

            val newHash = updateHash(hash, move)
            val score = -quiescence(board, color.opponent, -beta, -currentAlpha, newHash, qDepth + 1)
            board.unmakeMove(move)

            if (searchAborted) return 0

            if (score >= beta) return score
            if (score > currentAlpha) currentAlpha = score
        }

        return currentAlpha
    }

    private fun updateHash(hash: Long, move: Move): Long {
        var h = hash
        h = h xor ZobristHash.pieceHash(move.piece, move.from.row, move.from.col)
        h = h xor ZobristHash.pieceHash(move.piece, move.to.row, move.to.col)
        if (move.captured != null) {
            h = h xor ZobristHash.pieceHash(move.captured, move.to.row, move.to.col)
        }
        h = h xor ZobristHash.sideHash()
        return h
    }

    private fun orderMovesForSearch(moves: List<Move>, ttBestMove: Int, ply: Int): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            if (sameMove(move, ttBestMove)) {
                score += 1_000_000
            }
            if (move.captured != null) {
                score += 100_000 + captureValue(move)
            }
            if (ply < MAX_PLY && move.captured == null) {
                if (sameMove(move, killerMoves[ply][0])) score += 90_000
                else if (sameMove(move, killerMoves[ply][1])) score += 80_000
            }
            if (move.captured == null) {
                val ci = if (move.piece.color == PieceColor.RED) 0 else 1
                score += historyTable[ci][move.from.row][move.from.col][move.to.row][move.to.col]
            }
            score
        }
    }

    private fun captureValue(move: Move): Int {
        val victimValue = if (move.captured != null) Evaluator.pieceValue(move.captured.type) else 0
        val attackerValue = Evaluator.pieceValue(move.piece.type)
        return victimValue * 10 - attackerValue
    }
}
