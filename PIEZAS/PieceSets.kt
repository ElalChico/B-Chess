/*
  PIEZAS — Libreria de conjuntos de piezas (CHESS-X)
  144 VectorDrawable oficiales de Lichess (12 conjuntos x 12 piezas)
  + codigo autocontenido para Android.

  Uso: ver README.md. El mapeo estatico pieceDrawableId() referencia
  R directamente, por lo que funciona con resource shrinking.
  Si prefieres no importar R, usa pieceDrawableIdByName()
  (NO sobrevive a shrinkResources).
*/
package com.piezas

// Descomenta y ajusta a tu paquete:
// import com.TU_APK.R

// ---------------------------------------------------------------------------
// Modelos propios (sin dependencias de ningun proyecto)
// ---------------------------------------------------------------------------

enum class ChessColor { WHITE, BLACK

    fun code(): Char = if (this == WHITE) 'w' else 'b'
}

enum class ChessPiece { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING

    fun code(): Char = when (this) {
        PAWN -> 'p'; KNIGHT -> 'n'; BISHOP -> 'b'
        ROOK -> 'r'; QUEEN -> 'q'; KING -> 'k'
    }
}

/**
 * Conjuntos de piezas disponibles.
 *
 * Todos son los juegos oficiales de Lichess, convertidos a VectorDrawable.
 * Cada conjunto son 12 vectores: drawable/ps_<resKey>_<w|b><pieza>.xml
 */
enum class PieceSet(val labelEs: String, val labelEn: String, val resKey: String) {
    LICHESS("Clásico", "Classic", "cburnett"),
    ALPHA("Alpha", "Alpha", "alpha"),
    ANARCANDY("Anarcandy", "Anarcandy", "anarcandy"),
    COMPANION("Cómplice", "Companion", "companion"),
    HORSEY("Horsey", "Horsey", "horsey"),
    LEIPZIG("Leipzig", "Leipzig", "leipzig"),
    MERIDA("Merida", "Merida", "merida"),
    PIXEL("Pixel", "Pixel", "pixel"),
    RIOHACHA("Riohacha", "Riohacha", "riohacha"),
    STAUNTY("Staunty", "Staunty", "staunty"),
    TATIANA("Tatiana", "Tatiana", "tatiana"),
    ICPIECES("Icpieces", "Icpieces", "icpieces");

    companion object {
        val DEFAULT = LICHESS
    }
}

// ---------------------------------------------------------------------------
// Mapeo estatico de recursos (rapido y compatible con resource shrinking)
// Asegurate de tener los 144 drawables en res/drawable/ con los mismos nombres.
// ---------------------------------------------------------------------------

fun pieceDrawableId(piece: ChessPiece, color: ChessColor, pieceSet: PieceSet = PieceSet.DEFAULT): Int =
    when (pieceSet) {
        PieceSet.LICHESS -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_cburnett_wp
            "bp" -> R.drawable.ps_cburnett_bp
            "wn" -> R.drawable.ps_cburnett_wn
            "bn" -> R.drawable.ps_cburnett_bn
            "wb" -> R.drawable.ps_cburnett_wb
            "bb" -> R.drawable.ps_cburnett_bb
            "wr" -> R.drawable.ps_cburnett_wr
            "br" -> R.drawable.ps_cburnett_br
            "wq" -> R.drawable.ps_cburnett_wq
            "bq" -> R.drawable.ps_cburnett_bq
            "wk" -> R.drawable.ps_cburnett_wk
            "bk" -> R.drawable.ps_cburnett_bk
            else -> R.drawable.ps_cburnett_bk
        }
        PieceSet.ALPHA -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_alpha_wp
            "bp" -> R.drawable.ps_alpha_bp
            "wn" -> R.drawable.ps_alpha_wn
            "bn" -> R.drawable.ps_alpha_bn
            "wb" -> R.drawable.ps_alpha_wb
            "bb" -> R.drawable.ps_alpha_bb
            "wr" -> R.drawable.ps_alpha_wr
            "br" -> R.drawable.ps_alpha_br
            "wq" -> R.drawable.ps_alpha_wq
            "bq" -> R.drawable.ps_alpha_bq
            "wk" -> R.drawable.ps_alpha_wk
            "bk" -> R.drawable.ps_alpha_bk
            else -> R.drawable.ps_alpha_bk
        }
        PieceSet.ANARCANDY -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_anarcandy_wp
            "bp" -> R.drawable.ps_anarcandy_bp
            "wn" -> R.drawable.ps_anarcandy_wn
            "bn" -> R.drawable.ps_anarcandy_bn
            "wb" -> R.drawable.ps_anarcandy_wb
            "bb" -> R.drawable.ps_anarcandy_bb
            "wr" -> R.drawable.ps_anarcandy_wr
            "br" -> R.drawable.ps_anarcandy_br
            "wq" -> R.drawable.ps_anarcandy_wq
            "bq" -> R.drawable.ps_anarcandy_bq
            "wk" -> R.drawable.ps_anarcandy_wk
            "bk" -> R.drawable.ps_anarcandy_bk
            else -> R.drawable.ps_anarcandy_bk
        }
        PieceSet.COMPANION -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_companion_wp
            "bp" -> R.drawable.ps_companion_bp
            "wn" -> R.drawable.ps_companion_wn
            "bn" -> R.drawable.ps_companion_bn
            "wb" -> R.drawable.ps_companion_wb
            "bb" -> R.drawable.ps_companion_bb
            "wr" -> R.drawable.ps_companion_wr
            "br" -> R.drawable.ps_companion_br
            "wq" -> R.drawable.ps_companion_wq
            "bq" -> R.drawable.ps_companion_bq
            "wk" -> R.drawable.ps_companion_wk
            "bk" -> R.drawable.ps_companion_bk
            else -> R.drawable.ps_companion_bk
        }
        PieceSet.HORSEY -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_horsey_wp
            "bp" -> R.drawable.ps_horsey_bp
            "wn" -> R.drawable.ps_horsey_wn
            "bn" -> R.drawable.ps_horsey_bn
            "wb" -> R.drawable.ps_horsey_wb
            "bb" -> R.drawable.ps_horsey_bb
            "wr" -> R.drawable.ps_horsey_wr
            "br" -> R.drawable.ps_horsey_br
            "wq" -> R.drawable.ps_horsey_wq
            "bq" -> R.drawable.ps_horsey_bq
            "wk" -> R.drawable.ps_horsey_wk
            "bk" -> R.drawable.ps_horsey_bk
            else -> R.drawable.ps_horsey_bk
        }
        PieceSet.LEIPZIG -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_leipzig_wp
            "bp" -> R.drawable.ps_leipzig_bp
            "wn" -> R.drawable.ps_leipzig_wn
            "bn" -> R.drawable.ps_leipzig_bn
            "wb" -> R.drawable.ps_leipzig_wb
            "bb" -> R.drawable.ps_leipzig_bb
            "wr" -> R.drawable.ps_leipzig_wr
            "br" -> R.drawable.ps_leipzig_br
            "wq" -> R.drawable.ps_leipzig_wq
            "bq" -> R.drawable.ps_leipzig_bq
            "wk" -> R.drawable.ps_leipzig_wk
            "bk" -> R.drawable.ps_leipzig_bk
            else -> R.drawable.ps_leipzig_bk
        }
        PieceSet.MERIDA -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_merida_wp
            "bp" -> R.drawable.ps_merida_bp
            "wn" -> R.drawable.ps_merida_wn
            "bn" -> R.drawable.ps_merida_bn
            "wb" -> R.drawable.ps_merida_wb
            "bb" -> R.drawable.ps_merida_bb
            "wr" -> R.drawable.ps_merida_wr
            "br" -> R.drawable.ps_merida_br
            "wq" -> R.drawable.ps_merida_wq
            "bq" -> R.drawable.ps_merida_bq
            "wk" -> R.drawable.ps_merida_wk
            "bk" -> R.drawable.ps_merida_bk
            else -> R.drawable.ps_merida_bk
        }
        PieceSet.PIXEL -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_pixel_wp
            "bp" -> R.drawable.ps_pixel_bp
            "wn" -> R.drawable.ps_pixel_wn
            "bn" -> R.drawable.ps_pixel_bn
            "wb" -> R.drawable.ps_pixel_wb
            "bb" -> R.drawable.ps_pixel_bb
            "wr" -> R.drawable.ps_pixel_wr
            "br" -> R.drawable.ps_pixel_br
            "wq" -> R.drawable.ps_pixel_wq
            "bq" -> R.drawable.ps_pixel_bq
            "wk" -> R.drawable.ps_pixel_wk
            "bk" -> R.drawable.ps_pixel_bk
            else -> R.drawable.ps_pixel_bk
        }
        PieceSet.RIOHACHA -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_riohacha_wp
            "bp" -> R.drawable.ps_riohacha_bp
            "wn" -> R.drawable.ps_riohacha_wn
            "bn" -> R.drawable.ps_riohacha_bn
            "wb" -> R.drawable.ps_riohacha_wb
            "bb" -> R.drawable.ps_riohacha_bb
            "wr" -> R.drawable.ps_riohacha_wr
            "br" -> R.drawable.ps_riohacha_br
            "wq" -> R.drawable.ps_riohacha_wq
            "bq" -> R.drawable.ps_riohacha_bq
            "wk" -> R.drawable.ps_riohacha_wk
            "bk" -> R.drawable.ps_riohacha_bk
            else -> R.drawable.ps_riohacha_bk
        }
        PieceSet.STAUNTY -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_staunty_wp
            "bp" -> R.drawable.ps_staunty_bp
            "wn" -> R.drawable.ps_staunty_wn
            "bn" -> R.drawable.ps_staunty_bn
            "wb" -> R.drawable.ps_staunty_wb
            "bb" -> R.drawable.ps_staunty_bb
            "wr" -> R.drawable.ps_staunty_wr
            "br" -> R.drawable.ps_staunty_br
            "wq" -> R.drawable.ps_staunty_wq
            "bq" -> R.drawable.ps_staunty_bq
            "wk" -> R.drawable.ps_staunty_wk
            "bk" -> R.drawable.ps_staunty_bk
            else -> R.drawable.ps_staunty_bk
        }
        PieceSet.TATIANA -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_tatiana_wp
            "bp" -> R.drawable.ps_tatiana_bp
            "wn" -> R.drawable.ps_tatiana_wn
            "bn" -> R.drawable.ps_tatiana_bn
            "wb" -> R.drawable.ps_tatiana_wb
            "bb" -> R.drawable.ps_tatiana_bb
            "wr" -> R.drawable.ps_tatiana_wr
            "br" -> R.drawable.ps_tatiana_br
            "wq" -> R.drawable.ps_tatiana_wq
            "bq" -> R.drawable.ps_tatiana_bq
            "wk" -> R.drawable.ps_tatiana_wk
            "bk" -> R.drawable.ps_tatiana_bk
            else -> R.drawable.ps_tatiana_bk
        }
        PieceSet.ICPIECES -> when ("${color.code()}${piece.code()}") {
            "wp" -> R.drawable.ps_icpieces_wp
            "bp" -> R.drawable.ps_icpieces_bp
            "wn" -> R.drawable.ps_icpieces_wn
            "bn" -> R.drawable.ps_icpieces_bn
            "wb" -> R.drawable.ps_icpieces_wb
            "bb" -> R.drawable.ps_icpieces_bb
            "wr" -> R.drawable.ps_icpieces_wr
            "br" -> R.drawable.ps_icpieces_br
            "wq" -> R.drawable.ps_icpieces_wq
            "bq" -> R.drawable.ps_icpieces_bq
            "wk" -> R.drawable.ps_icpieces_wk
            "bk" -> R.drawable.ps_icpieces_bk
            else -> R.drawable.ps_icpieces_bk
        }    }

/**
 * Mapeo dinamico por nombre (no usa R, pero NO sobrevive a resource shrinking).
 * Devuelve 0 si no encuentra el recurso.
 */
fun pieceDrawableIdByName(
    context: android.content.Context,
    piece: ChessPiece,
    color: ChessColor,
    pieceSet: PieceSet = PieceSet.DEFAULT
): Int {
    val key = "ps_${pieceSet.resKey}_${color.code()}${piece.code()}"
    val cached = byNameCache[key]
    if (cached != null) return cached
    val id = context.resources.getIdentifier(key, "drawable", context.packageName)
    byNameCache[key] = id
    return id
}

private val byNameCache = HashMap<String, Int>()
