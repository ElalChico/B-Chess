package com.bchess.android.data

import java.util.UUID

/**
 * Extensiones para GameRecord con campos adicionales para estadísticas completas
 * (motivo de fin, SAN real, modo de guardado, etc.)
 */
data class GameRecordExtended(
    val base: GameRecord,
    val endReason: String? = null,           // "CHECKMATE", "TIMEOUT", "RESIGNATION", "DRAW_AGREED", etc.
    val saveMode: String = "auto",           // "auto" | "manual" | "never" - para Stockfish
    val movesCount: Int = 0,                 // Total de movimientos (SAN)
    val whitePlayer: String = "",            // Nombre jugador con blancas
    val blackPlayer: String = "",            // Nombre jugador con negras
    val localResultLabel: String = ""        // "VICTORIA" | "DERROTA" | "TABLAS" para el jugador local
) {
    val id: String = base.id
    val dateMs: Long = base.dateMs
    val opponent: String = base.opponent
    val result: String = base.result
    val localWon: Boolean? = base.localWon
    val localColorWhite: Boolean = base.localColorWhite
    val timeLabel: String = base.timeLabel
    val movesSan: List<String> = base.movesSan
    val pgn: String = base.pgn
    val finalFen: String = base.finalFen
    val playerName: String = base.playerName
    val uciMoves: List<String> = base.uciMoves
    val moveClocks: List<Long> = base.moveClocks
    val opponentPhotoBase64: String? = base.opponentPhotoBase64
    val gameType: String = base.gameType
    val localIsHost: Boolean? = base.localIsHost

    fun dateLabel(): String = base.dateLabel()

    companion object {
        fun fromGameRecord(record: GameRecord, endReason: String? = null, saveMode: String? = null): GameRecordExtended {
            val movesCount = record.movesSan.size
            val (whitePlayer, blackPlayer) = if (record.localColorWhite) {
                record.playerName.ifBlank { "Tú" } to record.opponent
            } else {
                record.opponent to record.playerName.ifBlank { "Tú" }
            }
            val localResultLabel = when (record.localWon) {
                true -> "VICTORIA"
                false -> "DERROTA"
                null -> "TABLAS"
            }
            return GameRecordExtended(
                base = record,
                endReason = endReason ?: record.endReason,
                saveMode = saveMode ?: record.saveMode,
                movesCount = movesCount,
                whitePlayer = whitePlayer,
                blackPlayer = blackPlayer,
                localResultLabel = localResultLabel
            )
        }
    }
}