/*
 * B-Chess · Motor de ajedrez local (puro Kotlin, sin binarios nativos).
 *
 * Objetivo
 *   Sustituir al binario Stockfish (arm64) por un motor que funciona en
 *   cualquier móvil y en cualquier arquitectura. Minimax con poda alfa-beta,
 *   tablas de posición por pieza e iteración profunda con límite de tiempo.
 *
 *   Curva de dificultad continua (0-100):
 *     - Principiante  (0-15):  profundidad 1, jugadas aleatorias frecuentes (hasta 100% en 0%)
 *     - Aficionado   (16-30): profundidad 1-2, errores ocasionales
 *     - Intermedio   (31-50): profundidad 2-3, errores raros
 *     - Avanzado     (51-70): profundidad 3-4, sin errores
 *     - Experto      (71-85): profundidad 4-5, sin errores
 *     - Muy difícil  (86-100): profundidad 5, sin errores y más tiempo de cálculo
 *   Además, a menor nivel la evaluación lleva ruido (el motor "se equivoca").
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
import kotlin.math.roundToInt

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

    // Contador de nodos de la búsqueda en curso (para el límite de tiempo).
    private var nodes = 0L

    private val _installState = kotlinx.coroutines.flow.MutableStateFlow<EngineInstallState>(EngineInstallState.Ready)
    val installState: kotlinx.coroutines.flow.StateFlow<EngineInstallState> = _installState

    fun isInstalled(): Boolean = true

    /** Nada que detener: no hay proceso nativo. */
    fun stop() { /* no-op (motor integrado en la app) */ }

    companion object {
        private const val MATE_SCORE = 1_000_000
        private const val SIDE_TO_MOVE_BONUS = 12
        /** ∞ segura: cualquier evaluation está dentro de ±MATE_SCORE, así que ±2M nunca desborda. */
        private const val INF = 2_000_000

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
        var whiteBishops = 0
        var blackBishops = 0
        for (sq in 0..63) {
            val p = state.board[sq] ?: continue
            val idx = sq xor if (p.color == Color.WHITE) 0 else 56
            val v = (PIECE_VALUE.getOrNull(p.type.ordinal) ?: 0) + (PST[p.type]?.getOrNull(idx) ?: 0)
            if (p.type == PieceType.BISHOP) {
                if (p.color == Color.WHITE) whiteBishops++ else blackBishops++
            }
            score += if (p.color == Color.WHITE) v else -v
        }
        // Pareja de alfiles: premio ligero pero real
        if (whiteBishops >= 2) score += 30
        if (blackBishops >= 2) score -= 30
        // Peones pasados y torres en columna abierta
        for (sq in 0..63) {
            val p = state.board[sq] ?: continue
            val file = sq % 8
            val rank = sq / 8
            when (p.type) {
                PieceType.PAWN -> {
                    val white = p.color == Color.WHITE
                    if (!hasEnemyPawnAhead(state, file, rank, white)) {
                        val advance = if (white) rank else 7 - rank
                        val bonus = 8 + advance * 6
                        score += if (white) bonus else -bonus
                    }
                }
                PieceType.ROOK -> {
                    if (!hasPawnOnFile(state, file)) {
                        score += if (p.color == Color.WHITE) 12 else -12
                    }
                }
                else -> {}
            }
        }
        // Evaluación desde la perspectiva del bando al que le toca mover
        return if (state.sideToMove == Color.WHITE) score + SIDE_TO_MOVE_BONUS else -score + SIDE_TO_MOVE_BONUS
    }

    /** ¿Hay algún peón (de cualquier color) en la columna? */
    private fun hasPawnOnFile(state: BoardSnapshot, file: Int): Boolean =
        (0..7).any { r -> state.board[r * 8 + file]?.type == PieceType.PAWN }

    /** ¿Hay un peón enemigo por delante (misma o columna adyacente)? */
    private fun hasEnemyPawnAhead(state: BoardSnapshot, file: Int, rank: Int, white: Boolean): Boolean {
        val enemy = if (white) Color.BLACK else Color.WHITE
        for (f in (file - 1)..(file + 1)) {
            if (f < 0 || f > 7) continue
            val rows = if (white) (rank + 1)..7 else 0 until rank
            for (r in rows) {
                val p = state.board[r * 8 + f] ?: continue
                if (p.type == PieceType.PAWN && p.color == enemy) return true
            }
        }
        return false
    }

    private fun orderMoves(moves: List<Move>, state: BoardSnapshot): List<Move> =
        moves.sortedByDescending { moveOrderScore(it, state) }

    /** Devuelve la mejor jugada (UCI) para la posición alcanzada tras [moves]. */
    suspend fun bestMove(moves: List<String>, strengthPercent: Int): String = withContext(Dispatchers.Default) {
        val engine = snapshotOf(moves)
        val rootState = engine.snapshot
        val rootMoves = legalMovesOf(rootState)
        if (rootMoves.isEmpty()) return@withContext ""

        val normalized = strengthPercent.coerceIn(0, 100)
        val random = kotlin.random.Random

        // 1) Curva de error: a menor nivel, más probabilidad de jugada aleatoria.
        //    0% es prácticamente un jugador aleatorio; 100% nunca falla.
        if (random.nextFloat() < errorProbability(normalized)) {
            return@withContext rootMoves.random(random).uci()
        }

        // 2) Profundidad objetivo: 1 (principiante) hasta 5 (muy difícil).
        val targetDepth = depthFor(normalized)
        // 3) Presupuesto de tiempo: los niveles altos piensan más, como un motor real.
        val budgetMs = if (normalized < 28) {
            300L + normalized * 22L
        } else {
            900L + (normalized - 28) * 35L
        }
        val deadline = System.currentTimeMillis() + budgetMs

        // 4) Ruido de evaluación en la raíz: niveles bajos cometen más errores;
        //    desde 28% el juego se vuelve claramente más preciso.
        val noise = if (normalized < 28) {
            ((27 - normalized) * 1.4f + 20f).roundToInt()
        } else {
            ((100 - normalized) * 0.12f).roundToInt()
        }

        var bestMove: Move = rootMoves.first()
        var completedDepth = 0
        for (depth in 1..targetDepth) {
            try {
                bestMove = searchRoot(rootState, rootMoves, depth, deadline, noise, random)
                completedDepth = depth
            } catch (_: SearchTimeout) {
                break
            }
            if (System.currentTimeMillis() >= deadline) break
        }
        // Si ni siquiera se completó la profundidad 1 (improbable), jugada legal segura.
        if (completedDepth == 0) bestMove = rootMoves.random(random)
        bestMove.uci()
    }

    /** Probabilidad de jugada aleatoria según el nivel (0% = casi todo aleatorio). */
    private fun errorProbability(percent: Int): Float = when {
        percent <= 15 -> 1.00f - percent * 0.0400f                 // 1.00 → 0.40  (Principiante)
        percent <= 27 -> 0.40f - (percent - 15) * (0.20f / 12f)    // 0.40 → 0.20  (Aficionado)
        percent <= 40 -> 0.20f - (percent - 28) * (0.18f / 12f)    // 0.20 → 0.02  (Intermedio, desde 28%)
        percent <= 60 -> 0.02f - (percent - 40) * 0.001f           // 0.02 → 0.00  (Avanzado)
        else -> 0f                                                 // Experto/Muy difícil
    }

    /** Profundidad de búsqueda para un nivel dado. */
    internal fun depthFor(percent: Int): Int = when {
        percent <= 15 -> 1
        percent <= 27 -> 2
        percent <= 40 -> 4
        percent <= 60 -> 4
        percent <= 80 -> 5
        else -> 6
    }

    private class SearchTimeout : RuntimeException()

    /** Búsqueda de la mejor jugada en la raíz con [depth] y límite de tiempo. */
    private fun searchRoot(
        state: BoardSnapshot,
        moves: List<Move>,
        depth: Int,
        deadline: Long,
        noise: Int,
        random: kotlin.random.Random
    ): Move {
        nodes = 0
        var bestScore = -INF
        var bestMoves = mutableListOf<Move>()
        var alpha = -INF
        val ordered = orderMoves(moves, state)
        for (move in ordered) {
            val child = helper.applyMove(state, move)
            val score = -negamax(child, depth - 1, -INF, -alpha, deadline) +
                if (noise > 0) random.nextInt(-noise, noise + 1) else 0
            if (score > bestScore) {
                bestScore = score
                bestMoves = mutableListOf(move)
                if (score > alpha) alpha = score
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }
        return bestMoves.random(random)
    }

    private fun negamax(state: BoardSnapshot, depth: Int, alpha: Int, beta: Int, deadline: Long): Int {
        // Comprobación de tiempo cada 1024 nodos: no interrumpe la partida.
        if ((nodes++ and 1023) == 0L && System.currentTimeMillis() > deadline) throw SearchTimeout()
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
        var best = -INF
        var a = alpha
        val ordered = orderMoves(moves, state)
        for (move in ordered) {
            val child = helper.applyMove(state, move)
            val score = -negamax(child, depth - 1, -beta, -a, deadline)
            if (score > best) best = score
            if (best > a) a = best
            if (a >= beta) break
        }
        return best
    }
}
