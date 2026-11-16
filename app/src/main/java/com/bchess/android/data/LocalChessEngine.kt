/*
 * B-Chess · Motor de ajedrez local (puro Kotlin, sin binarios nativos).
 *
 * Objetivo
 *   Sustituir al binario Stockfish (arm64) por un motor que funciona en
 *   cualquier móvil y en cualquier arquitectura. Minimax con poda alfa-beta
 *   y tablas de posición por pieza. Tres niveles de dificultad desde el
 *   más principiante hasta el más avanzado:
 *     - Principiante (0-30): búsqueda profundidad 1 + jugadas aleatorias frecuentes
 *     - Intermedio   (31-65): búsqueda profundidad 2
 *     - Avanzado     (66-100): búsqueda profundidad 3
 *   Sin binarios: funciona en todos los teléfonos, incluidos de bajos recursos.
 */

package com.bchess.android.data

import com.bchess.android.core.ChessEngine
import com.bchess.android.core.BoardSnapshot
import com.bchess.android.core.Color
import com.bchess.android.core.Move
import com.bchess.android.core.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface EngineInstallState {
    object Idle : EngineInstallState
    object Ready : EngineInstallState
    data class Error(val message: String) : EngineInstallState
}

/** Motor de ajedrez local con minimax + alfa-beta. */
class LocalChessEngine {

    // Motor auxiliar reutilizado con loadSnapshot() para generar y aplicar
    // jugadas sobre una posición arbitraria (no muta el estado real).
    private val helper = ChessEngine()

    private val _installState = kotlinx.coroutines.flow.MutableStateFlow<EngineInstallState>(EngineInstallState.Ready)
    val installState: kotlinx.coroutines.flow.StateFlow<EngineInstallState> = _installState

    fun isInstalled(): Boolean = true

    /** Nada que detener: no hay proceso nativo. */
    fun stop() { /* no-op (motor integrado en la app) */ }

    companion object {
        private const val MATE_SCORE = 1_000_000
        private const val SIDE_TO_MOVE_BONUS = 12

        private val PIECE_VALUE = intArrayOf(100, 320, 330, 500, 900, 0) // ordinal: PAWN..KING

        // Tablas de posición por tipo de pieza (valor por casilla, desde el punto
        // de vista de blancas; a1 = índice 0 .. h8 = índice 63). Compactas pero
        // suficientes para una fuerza razonable.
        private val PAWN_TABLE = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
        )

        private val KNIGHT_TABLE = intArrayOf(
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
        )

        private val BISHOP_TABLE = intArrayOf(
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
        )

        private val ROOK_TABLE = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            0, 0, 0, 5, 5, 0, 0, 0
        )

        private val QUEEN_TABLE = intArrayOf(
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
        )

        private val KING_TABLE = intArrayOf(
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 30, 0, 0, 0, 0, 30, 20,
            20, 30, 10, 0, 0, 10, 30, 20
        )

        private val PST = mapOf(
            PieceType.PAWN to PAWN_TABLE,
            PieceType.KNIGHT to KNIGHT_TABLE,
            PieceType.BISHOP to BISHOP_TABLE,
            PieceType.ROOK to ROOK_TABLE,
            PieceType.QUEEN to QUEEN_TABLE,
            PieceType.KING to KING_TABLE
        )

        /** Piezas que animan el orden de jugadas para la poda alfa-beta. */
        private fun moveOrderScore(move: Move, state: BoardSnapshot): Int {
            var s = 0
            if (move.isCapture) {
                val victim = state.board[move.to]
                val attacker = state.board[move.from]
                // MVV-LVA: víctima más valiosa primero; atacante menos valioso desempata
                s += (victim?.let { PIECE_VALUE.getOrNull(it.type.ordinal) } ?: 0) * 10
                s -= (attacker?.let { PIECE_VALUE.getOrNull(it.type.ordinal) } ?: 0)
            }
            if (move.promotion != null) s += 9000
            return s
        }

        @Volatile
        private var instance: LocalChessEngine? = null

        fun getInstance(context: android.content.Context): LocalChessEngine =
            instance ?: synchronized(this) {
                instance ?: LocalChessEngine().also { instance = it }
            }
    }

    /** Aplica la secuencia de jugadas UCI sobre un motor fresco y devuelve la posición resultante. */
    private fun snapshotOf(moves: List<String>): ChessEngine {
        val engine = ChessEngine()
        engine.reset()
        for (uci in moves) {
            val from = uci?.substring(0, 2)?.let { Move.algebraicToSquare(it) } ?: continue
            val to = uci.substring(2, 4).let { Move.algebraicToSquare(it) } ?: continue
            val promo = when (uci.getOrNull(4)) {
                'q' -> PieceType.QUEEN; 'r' -> PieceType.ROOK; 'b' -> PieceType.BISHOP; 'n' -> PieceType.KNIGHT
                else -> null
            }
            val move = engine.findMove(from, to, promo) ?: continue
            engine.doMove(move)
        }
        return engine
    }

    private fun legalMovesOf(state: BoardSnapshot): List<Move> {
        helper.loadSnapshot(state)
        return helper.legalMoves()
    }

    private fun evaluate(state: BoardSnapshot): Int {
        var score = 0
        for (sq in 0..63) {
            val p = state.board[sq] ?: continue
            val idx = sq xor if (p.color == Color.WHITE) 0 else 56
            val v = (PIECE_VALUE.getOrNull(p.type.ordinal) ?: 0) + (PST[p.type]?.getOrNull(idx) ?: 0)
            score += if (p.color == Color.WHITE) v else -v
        }
        // Evaluación desde la perspectiva del bando al que le toca mover
        return if (state.sideToMove == Color.WHITE) score + SIDE_TO_MOVE_BONUS else -score + SIDE_TO_MOVE_BONUS
    }

    private fun orderMoves(moves: List<Move>, state: BoardSnapshot): List<Move> =
        moves.sortedByDescending { moveOrderScore(it, state) }

    private fun negamax(state: BoardSnapshot, depth: Int, alpha: Int, beta: Int): Int {
        val moves = legalMovesOf(state)
        // Fin de la partida: mate o tablas sin jugadas legales
        if (moves.isEmpty()) {
            val inCheck = helper.isInCheck(state.sideToMove, state)
            return if (inCheck) -MATE_SCORE + (100 - depth) else 0 // prefiere mates rápidos
        }
        if (depth == 0) {
            // Tablas por material insuficiente: 0
            return evaluate(state)
        }
        var best = Int.MIN_VALUE
        var a = alpha
        val ordered = orderMoves(moves, state)
        for (move in ordered) {
            val child = helper.applyMove(state, move)
            val score = -negamax(child, depth - 1, -beta, -a)
            if (score > best) best = score
            if (best > a) a = best
            if (a >= beta) break
        }
        return best
    }

    /** Devuelve la mejor jugada (UCI) para la posición alcanzada tras [moves]. */
    suspend fun bestMove(moves: List<String>, strengthPercent: Int): String = withContext(Dispatchers.Default) {
        val engine = snapshotOf(moves)
        val rootState = engine.snapshot
        val rootMoves = legalMovesOf(rootState)
        if (rootMoves.isEmpty()) return@withContext ""

        val normalized = strengthPercent.coerceIn(0, 100)

        // Principiante: con frecuencia juega al azar (el motor "se equivoca")
        val random = kotlin.random.Random
        if (normalized <= 30 && random.nextFloat() < 0.35f) {
            return@withContext rootMoves.random(random).uci()
        }

        val depth = when {
            normalized <= 30 -> 1
            normalized <= 65 -> 2
            else -> 3
        }

        // Intermedio: pequeña probabilidad de error por variedad
        if (normalized <= 65 && random.nextFloat() < 0.08f) {
            return@withContext rootMoves.random(random).uci()
        }

        // Ordena y evalúa las jugadas raíz con poda alfa-beta
        var bestScore = Int.MIN_VALUE
        var bestMoves = mutableListOf<Move>()
        var alpha = Int.MIN_VALUE
        val ordered = orderMoves(rootMoves, rootState)
        for (move in ordered) {
            val child = helper.applyMove(rootState, move)
            val score = -negamax(child, depth - 1, Int.MIN_VALUE, -alpha)
            if (score > bestScore) {
                bestScore = score
                bestMoves = mutableListOf(move)
                alpha = score
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }
        val chosen = bestMoves.random(random)
        chosen.uci()
    }
}
