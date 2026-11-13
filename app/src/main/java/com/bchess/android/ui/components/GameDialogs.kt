/*
 * CHESS-X · © Elal Chico
 * Diálogos de finalización: fin de partida (mismo color / nuevo color / torneo),
 * fin de torneo (marcador) y selección de pieza al promover.
 */

package com.bchess.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bchess.android.R
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.core.GameEnd
import com.bchess.android.core.Piece
import com.bchess.android.core.PieceType
import com.bchess.android.data.PieceSet
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

private fun decodePhoto(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    val raw = base64.substringAfter("base64,", base64)
    return try {
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) { null }
}

// ---------- Fin de partida ----------

@Composable
fun GameEndDialog(
    end: GameEnd,
    isHost: Boolean,
    tournamentActive: Boolean,
    tournamentFinished: Boolean,
    localName: String,
    remoteName: String,
    localPhotoBase64: String = "",
    remotePhotoBase64: String? = null,
    winnerName: String? = null,
    onNewGameSame: () -> Unit,
    onNewGameRandom: () -> Unit,
    onTournamentNext: () -> Unit,
    onBackToRoom: () -> Unit,
    onReplayPgn: () -> Unit,
    onDismiss: () -> Unit
) {
    val resultColorStr = if (end.winner == null) "draw" else
        (if (end.winner == ChessColor.WHITE) "white" else "black")

    val winnerDisplayName = winnerName ?: (when (resultColorStr) {
        "white" -> if (localName.isNotBlank()) localName else "Blancas"
        "black" -> if (remoteName.isNotBlank()) remoteName else "Negras"
        else -> "Empate"
    })

    val winnerPhoto = when (resultColorStr) {
        "white" -> localPhotoBase64
        "black" -> remotePhotoBase64
        else -> null
    }

    val resultIcon = when (resultColorStr) {
        "white" -> "🏆"
        "black" -> "🏆"
        else -> "🤝"
    }
    val resultBgColor = when (resultColorStr) {
        "white" -> Color(0xFF2E1B1B)
        "black" -> Color(0xFF1B2E4E)
        else -> Color(0xFF1B2E2E)
    }
    val resultBorderColor = when (resultColorStr) {
        "white" -> Color(0xFFFF6B6B)
        "black" -> Color(0xFF5B9DF9)
        else -> Color(0xFF3DDC97)
    }
    val resultTitleColor = when (resultColorStr) {
        "white" -> Color(0xFFFF6B6B)
        "black" -> Color(0xFF5B9DF9)
        else -> Color(0xFF3DDC97)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1219).copy(alpha = 0.93f), RoundedCornerShape(24.dp))
                .border(2.dp, Color(0xFF1F2430), RoundedCornerShape(24.dp))
                .shadow(4.dp, CircleShape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Winner announcement card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(resultBgColor, RoundedCornerShape(20.dp))
                        .border(2.dp, resultBorderColor, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Result icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(resultBorderColor.copy(alpha = 0.15f), CircleShape)
                                .border(2.dp, resultBorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(resultIcon, fontSize = 36.sp)
                        }
                        
                        Text(
                            text = when (resultColorStr) {
                                "white" -> "¡BLANCAS GANAN!"
                                "black" -> "¡NEGRAS GANAN!"
                                else -> "¡TABLAS!"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = resultTitleColor,
                            textAlign = TextAlign.Center
                        )
                        
                        if (resultColorStr != "draw") {
                            Text(
                                text = winnerDisplayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE8EAF0),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Ambos jugadores empataron",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8B96A5),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = end.message.ifBlank { "Partida finalizada" },
                    fontSize = 13.sp,
                    color = Color(0xFF8B96A5),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                if (tournamentActive && !tournamentFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF26200F), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFC9B15A), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Torneo en curso · " + (if (isHost) "controlado por el anfitrión" else "controlado por el rival"),
                            fontSize = 11.5.sp,
                            color = Color(0xFFC9B15A),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                EndGameOption(
                    title = "Repetir partida",
                    subtitle = "Mismo color · misma cuenta",
                    enabled = true,
                    highlight = false,
                    onClick = onNewGameSame
                )
                EndGameOption(
                    title = "Nuevo color",
                    subtitle = "Aleatorio · siguiente partida",
                    enabled = true,
                    highlight = false,
                    onClick = onNewGameRandom
                )

                if (tournamentActive && !tournamentFinished) {
                    EndGameOption(
                        title = "Siguiente torneo",
                        subtitle = "Partida N+1 · color alternado",
                        enabled = true,
                        highlight = true,
                        onClick = onTournamentNext
                    )
                }
                EndGameOption(
                    title = "Reproducir PGN",
                    subtitle = "Ver la partida completa",
                    enabled = true,
                    highlight = false,
                    onClick = onReplayPgn
                )
                EndGameOption(
                    title = "Volver a la sala",
                    subtitle = "Continuar en el lobby del rival",
                    enabled = true,
                    highlight = false,
                    onClick = onBackToRoom
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161A23), contentColor = Color(0xFFEEF1F5))
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cerrar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3DDC97))
                        Spacer(Modifier.width(4.dp))
                        Text("→", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3DDC97))
                    }
                }
            }
        }
    }
}

@Composable
private fun EndGameOption(title: String, subtitle: String, enabled: Boolean, highlight: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enabled) Color(0xFF1B222C) else Color(0xFF12141A), RoundedCornerShape(16.dp))
            .border(1.dp, if (highlight) Color(0xFFC9B15A) else Color(0xFF2A3440), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Color(0xFFEEF1F5) else Color(0xFF6B6244)
            )
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF8B96A5))
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (highlight) Color(0xFF2E3A26) else Color(0xFF1E2A36), CircleShape)
                .border(1.dp, if (highlight) Color(0xFFC9B15A) else Color(0xFF2A3440), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = if (highlight) Color(0xFFC9B15A) else Color(0xFF8B96A5), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun TournamentEndDialog(
    totalGames: Int,
    localWins: Int,
    remoteWins: Int,
    draws: Int,
    timeControl: String,
    onNewTournament: () -> Unit,
    onBackToRoom: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = localWins + remoteWins + draws
    val winnerColor = when {
        localWins > remoteWins -> Color(0xFF3DDC97)
        remoteWins > localWins -> Color(0xFFFF6B6B)
        else -> Color(0xFF5B9DF9)
    }
    val winnerText = when {
        localWins > remoteWins -> "¡USTED GANA EL TORNEO!"
        remoteWins > localWins -> "¡GANA EL RIVAL!"
        else -> "TORNEO EMPATADO"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141A), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF3A3524), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF26200F), RoundedCornerShape(14.dp))
                        .border(1.dp, winnerColor, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TORNEO COMPLETADO", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = winnerColor, letterSpacing = 1.sp)
                        Text(
                            if (total == totalGames) "¡¡$winnerText $totalGames/$totalGames!!" else "Torneo finalizado",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC9B15A),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreRow("NEGRAS", remoteWins, Color(0xFF141414), Color(0xFFECECF4), remoteWins > localWins)
                    ScoreRow("BLANCAS", localWins, Color(0xFFECECF4), Color(0xFF141414), localWins > remoteWins)
                }
                Text("PARTIDAS: $total / $totalGames", fontSize = 12.sp, color = Color(0xFF8B96A5), fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onNewTournament,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Color(0xFFC9B15A))
                    ) { Text("Nuevo torneo", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9B15A)) }
                    Button(
                        onClick = onBackToRoom,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Color(0xFFEEF1F5))
                    ) { Text("Volver a la sala", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEEF1F5)) }
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(title: String, wins: Int, pieceColor: Color, badgeColor: Color, isWinning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isWinning) Color(0xFF26200F) else Color(0xFF151C25), RoundedCornerShape(12.dp))
            .border(1.dp, if (isWinning) Color(0xFFC9B15A) else Color(0xFF232C38), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B96A5), letterSpacing = 0.5.sp)
            Text(
                if (title == "BLANCAS") "Tus victorias: $wins" else "Tus victorias: $wins",
                fontSize = 10.5.sp,
                color = Color(0xFF8B96A5)
            )
        }
        Box(
            modifier = Modifier.size(44.dp).background(badgeColor, RoundedCornerShape(7.dp)).border(1.dp, Color(0xFF232C38), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(wins.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = pieceColor)
        }
    }
}

// ---------- Promoción ----------

private fun pieceRes(pieceType: PieceType, color: ChessColor): Int = when (pieceType) {
    PieceType.PAWN -> if (color == ChessColor.WHITE) R.drawable.piece_wp else R.drawable.piece_bp
    PieceType.KNIGHT -> if (color == ChessColor.WHITE) R.drawable.piece_wn else R.drawable.piece_bn
    PieceType.BISHOP -> if (color == ChessColor.WHITE) R.drawable.piece_wb else R.drawable.piece_bb
    PieceType.ROOK -> if (color == ChessColor.WHITE) R.drawable.piece_wr else R.drawable.piece_br
    PieceType.QUEEN -> if (color == ChessColor.WHITE) R.drawable.piece_wq else R.drawable.piece_bq
    PieceType.KING -> if (color == ChessColor.WHITE) R.drawable.piece_wk else R.drawable.piece_bk
}

@Composable
fun PromotionPieceDialog(
    localColor: ChessColor,
    pieceSet: PieceSet = PieceSet.DEFAULT,
    onPromote: (PieceType) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12141A), RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF3A3524), RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (localColor == ChessColor.WHITE) "Peón BLANCO promueve" else "Peón NEGRO promueve",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (localColor == ChessColor.WHITE) Color(0xFFECECF4) else Color(0xFF141414),
                    textAlign = TextAlign.Center
                )
                Text("Elige la pieza", fontSize = 12.5.sp, color = Color(0xFF8B96A5), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(PieceType.BISHOP, PieceType.ROOK, PieceType.KNIGHT, PieceType.QUEEN).forEach { pt ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { onPromote(pt) }
                        ) {
                            if (pieceSet == PieceSet.LICHESS || pieceSet.resKey != null) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = pieceResource(LocalContext.current, Piece(pt, localColor), pieceSet)),
                                    contentDescription = pt.name,
                                    modifier = Modifier.size(46.dp)
                                )
                            } else {
                                com.bchess.android.ui.components.PieceIcon(
                                    piece = Piece(pt, localColor),
                                    modifier = Modifier.size(46.dp),
                                    pieceSet = pieceSet
                                )
                            }
                            Text(
                                pt.name.take(1).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B96A5)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F))
                ) {
                    Text("Cancelar", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}
