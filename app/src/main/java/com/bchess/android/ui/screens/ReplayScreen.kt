package com.bchess.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import com.bchess.android.core.BoardSnapshot
import com.bchess.android.core.ChessEngine
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.core.Move
import com.bchess.android.core.PieceType
import com.bchess.android.data.GameRecord
import com.bchess.android.data.formatClock
import com.bchess.android.ui.components.BoardSelection
import com.bchess.android.ui.components.ChessBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Bg2 = Color(0xFF0B0D10)
private val Card3 = Color(0xFF171D25)
private val Line3 = Color(0xFF252E39)
private val Text13 = Color(0xFFEEF1F5)
private val Muted3 = Color(0xFF8B96A5)
private val Gold3 = Color(0xFFC9B15A)
private val Green3 = Color(0xFF3DDC97)

/**
 * Replay of a finished game from its PGN data: names, colors and per-move clocks.
 * Includes auto-play with configurable speed (1x/2x/3x).
 */
@Composable
fun ReplayScreen(record: GameRecord, showCoordinates: Boolean = true, onBack: () -> Unit) {
    var idx by remember { mutableIntStateOf(0) }
    var autoPlay by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableStateOf(1.0) } // 1x, 2x, 3x
    val total = record.uciMoves.size
    val snapshot: BoardSnapshot = remember(idx) {
        val engine = ChessEngine()
        engine.reset()
        repeat(idx) { i ->
            val uci = record.uciMoves.getOrNull(i) ?: return@repeat
            val promo = when (uci.lastOrNull()) {
                'q' -> PieceType.QUEEN; 'r' -> PieceType.ROOK; 'b' -> PieceType.BISHOP; 'n' -> PieceType.KNIGHT
                else -> null
            }
            val from = Move.algebraicToSquare(uci.substring(0, 2))
            val to = Move.algebraicToSquare(uci.substring(2, 4))
            if (from != null && to != null) engine.findMove(from, to, promo)?.let { engine.doMove(it) }
        }
        engine.snapshot
    }
    val localIsWhite = record.localColorWhite
    val localName = record.playerName
    val oppName = record.opponent
    val whiteClock = clockAfter(record, idx, true)
    val blackClock = clockAfter(record, idx, false)
    val localClock = if (localIsWhite) whiteClock else blackClock
    val remoteClock = if (localIsWhite) blackClock else whiteClock
    val san = record.movesSan.getOrNull(idx - 1)

    // Auto-play logic
    val scope = rememberCoroutineScope()
    LaunchedEffect(autoPlay, idx, playSpeed) {
        if (autoPlay && idx < total) {
            val delayMs = when (playSpeed) {
                2.0 -> 400L
                3.0 -> 200L
                else -> 800L
            }
            delay(delayMs)
            if (autoPlay) idx++
        } else if (autoPlay && idx >= total) {
            autoPlay = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg2).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                contentDescription = "Volver al historial",
                tint = Muted3,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Volver al historial", color = Muted3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Reproducción · $localName vs $oppName",
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Text13, maxLines = 1
                )
                Text(
                    "${record.dateLabel()} · ${record.timeLabel} · ${if (localIsWhite) "Juegas blancas" else "Juegas negras"} · ${record.result}",
                    fontSize = 11.sp, color = Muted3, maxLines = 1
                )
            }
        }
        HudBar(oppName, false, remoteClock, record)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val boardSize = minOf(maxWidth, maxHeight)
            ChessBoard(
                snapshot = snapshot,
                perspective = if (localIsWhite) ChessColor.WHITE else ChessColor.BLACK,
                selection = BoardSelection(null),
                onSquareTap = {},
                modifier = Modifier.size(boardSize),
                showCoordinates = showCoordinates
            )
        }
        HudBar(localName, true, localClock, record)

        // ==== CONTROLES DE REPRODUCCIÓN - BARRA COMPACTA ====
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121922),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold3.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fila superior: jugada actual y controles de velocidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        (san?.let { "Jugada $it" } ?: "Posición inicial") + "  ·  $idx/$total",
                        fontSize = 12.sp,
                        color = Muted3,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1.0, 2.0, 3.0).forEach { speed ->
                            val selected = playSpeed == speed
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) Gold3 else Color(0xFF1A212B),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(1.dp, if (selected) Gold3 else Line3, RoundedCornerShape(10.dp))
                                    .clickable { playSpeed = speed }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${speed.toInt()}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF17130A) else Text13
                                )
                            }
                        }
                    }
                }

                // Fila de transporte: iconos profesionales, sin emojis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlIcon(Icons.Default.SkipPrevious, "Ir al inicio", enabled = idx > 0) {
                        autoPlay = false; idx = 0
                    }
                    ControlIcon(Icons.Default.FastRewind, "Jugada anterior", enabled = idx > 0) {
                        autoPlay = false; if (idx > 0) idx--
                    }
                    ControlIcon(
                        if (autoPlay) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (autoPlay) "Pausar" else "Reproducir",
                        enabled = idx < total || autoPlay,
                        accent = true
                    ) {
                        if (autoPlay) {
                            autoPlay = false
                        } else {
                            if (idx >= total) idx = 0
                            autoPlay = true
                        }
                    }
                    ControlIcon(Icons.Default.FastForward, "Jugada siguiente", enabled = idx < total) {
                        autoPlay = false; if (idx < total) idx++
                    }
                    ControlIcon(Icons.Default.SkipNext, "Ir al final", enabled = idx < total) {
                        autoPlay = false; idx = total
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (accent) 54.dp else 44.dp
    val bg = when {
        accent -> Color(0xFFC9B15A)
        enabled -> Color(0xFF1A212B)
        else -> Color(0xFF14181F)
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(bg, CircleShape)
            .border(1.dp, if (accent) Color(0xFFF3D478) else Line3, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = when {
                accent -> Color(0xFF17130A)
                enabled -> Color(0xFFEEF1F5)
                else -> Color(0xFF8B96A5)
            },
            modifier = Modifier.size(if (accent) 30.dp else 22.dp)
        )
    }
}

private fun clockAfter(record: GameRecord, idx: Int, white: Boolean): Long {
    var last: Long? = null
    for (i in 0 until minOf(idx, record.moveClocks.size)) {
        val isWhiteMove = i % 2 == 0
        if (isWhiteMove == white) last = record.moveClocks[i]
    }
    return last ?: 0L
}

@Composable
private fun HudBar(name: String, isYou: Boolean, clockMs: Long, record: GameRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Card3, RoundedCornerShape(16.dp))
            .border(1.dp, Line3, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).background(
                if (isYou) Color(0xFF15251D) else Color(0xFF251A1A), androidx.compose.foundation.shape.CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (name.firstOrNull() ?: 'T').uppercaseChar().toString(),
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = if (isYou) Green3 else Color(0xFFFF9B9B)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(name, color = Text13, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
        Text(formatClock(clockMs), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Text13)
    }
}
