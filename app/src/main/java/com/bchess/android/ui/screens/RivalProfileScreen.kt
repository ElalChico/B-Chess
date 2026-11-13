package com.bchess.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.data.RemoteProfile
import com.bchess.android.data.RemoteGame
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import android.content.Intent
import android.net.Uri

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF171D25)
private val Card2 = Color(0xFF151C25)
private val Line = Color(0xFF252E39)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFC9B15A)
private val Green = Color(0xFF3DDC97)
private val Red = Color(0xFFFF6B6B)
private val GoldLight = Color(0xFFF3D478)

/**
 * Pantalla completa y profesional del perfil del adversario.
 * Respeta la configuración de privacidad del rival (sharePhoto, shareHistory, sharePgn).
 */
@Composable
fun RivalProfileScreen(
    profile: RemoteProfile,
    prefs: com.bchess.android.data.AppPreferences,
    onBack: () -> Unit,
    onReplayGame: (RemoteGame) -> Unit = {},
    context: androidx.compose.ui.platform.LocalContext? = null
) {
    val ctx = LocalContext.current

    // Mostrar lo que el rival envió (la privacidad del rival ya se respetó al enviar su perfil)
    val showPhoto = profile.photoBase64.isNotBlank()
    val showHistory = profile.games.isNotEmpty()
    val showPgnButtons = profile.games.any { it.pgn.isNotBlank() }

    val winRate = if (profile.total > 0) (profile.wins * 100) / profile.total else 0
    val drawRate = if (profile.total > 0) (profile.draws * 100) / profile.total else 0
    val lossRate = if (profile.total > 0) (profile.losses * 100) / profile.total else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Bg, Color(0xFF111821), Bg)
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // ==== HEADER CON BOTÓN VOLVER ====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Card, RoundedCornerShape(14.dp))
                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Text1,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "PERFIL DEL RIVAL",
                color = Text1,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Card, RoundedCornerShape(12.dp))
                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ==== SECCIÓN PRINCIPAL - FOTO Y NOMBRE ====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar grande y profesional
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2433))
                    .border(
                        3.dp,
                        if (showPhoto && profile.photoBase64.isNotBlank()) Gold else Line,
                        CircleShape
                    )
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (showPhoto) {
                    val bmp = remember(profile.photoBase64) {
                        try {
                            val raw = profile.photoBase64.substringAfter("base64,", profile.photoBase64)
                            val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp,
                            contentDescription = "Foto de ${profile.name}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold
                        )
                    }
                } else {
                    Text(
                        profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "R",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (sharePhoto) Gold else Muted.copy(alpha = 0.6f)
                    )
                    if (!sharePhoto) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .background(Color(0xFF0B0D10), CircleShape)
                                .border(2.dp, Line, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔒", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Nombre del rival
            Text(
                profile.name.ifBlank { "Rival sin nombre" },
                color = Text1,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            // Color de juego
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            when (profile.color) {
                                "white" -> Color(0xFFECECF4)
                                "black" -> Color(0xFF141414)
                                else -> Card2
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Line, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (profile.color) {
                            "white" -> "B"
                            "black" -> "N"
                            else -> "?"
                        },
                        color = when (profile.color) {
                            "white" -> Color(0xFF141414)
                            "black" -> Color(0xFFECECF4)
                            else -> Muted
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    when (profile.color) {
                        "white" -> "Juega con Blancas"
                        "black" -> "Juega con Negras"
                        "random" -> "Color aleatorio"
                        else -> "Color por definir"
                    },
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ==== ESTADÍSTICAS PRINCIPALES ====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "ESTADÍSTICAS",
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCardPro("Partidas", profile.total.toString(), Text1, Modifier.weight(1f))
                StatCardPro("Victorias", profile.wins.toString(), Green, Modifier.weight(1f))
                StatCardPro("Empates", profile.draws.toString(), Gold, Modifier.weight(1f))
                StatCardPro("Derrotas", profile.losses.toString(), Red, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // Tasa de victoria
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tasa de victoria:", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    "$winRate%",
                    color = if (winRate >= 60) Green else if (winRate >= 40) Gold else Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Barras de progreso visuales
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressRow("Victorias", winRate.toFloat(), Green)
                ProgressRow("Empates", drawRate.toFloat(), Gold)
                ProgressRow("Derrotas", lossRate.toFloat(), Red)
            }
        }

        Spacer(Modifier.height(24.dp))

        // ==== HISTORIAL DE PARTIDAS ====
        if (showHistory) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "HISTORIAL RECIENTE",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${profile.games.size} partidas",
                        color = Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.games.take(15).forEachIndexed { index, g ->
                        HistoryGameCard(
                            game = g,
                            showPgn = showPgnButtons && g.pgn.isNotBlank(),
                            showReplay = g.uciMoves.isNotEmpty(),
                            onReplay = { onReplayGame(g) },
                            onTogglePgn = { /* se maneja internamente */ }
                        )
                    }
                }
            }
        } else if (!shareHistory) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrivacyNoticeCard(
                    title = "Historial oculto",
                    message = "Este usuario no ha permitido compartir su historial de partidas. Puede cambiarlo en Ajustes → Compartir.",
                    icon = "📊",
                    color = Color(0xFF2A2A2A)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmptyStateCard(
                    title = "Sin partidas registradas",
                    message = "Este rival aún no tiene partidas guardadas en su historial.",
                    icon = "🏆"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ==== NOTA DE PRIVACIDAD (según lo que el rival compartió) ====
        val hiddenItems = mutableListOf<String>()
        if (!showPhoto) hiddenItems.add("foto de perfil")
        if (!showHistory) hiddenItems.add("historial de partidas")
        if (!showPgnButtons) hiddenItems.add("detalle de jugadas (PGN)")

        if (hiddenItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrivacyNoticeCard(
                    title = "Datos compartidos parcialmente",
                    message = "El rival no ha compartido: ${hiddenItems.joinToString(", ")}. Puede cambiarlo en su Ajustes → Compartir.",
                    icon = "🔒",
                    color = Color(0xFF2A242A)
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

/** Tarjeta de estadística profesional */
@Composable
private fun StatCardPro(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            value,
            color = valueColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            label,
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

/** Fila de progreso con barra visual */
@Composable
private fun ProgressRow(label: String, percent: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(Color(0xFF10151C), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(percent.coerceIn(0f, 100f) / 100f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${percent.toInt()}%",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
    }
}

/** Tarjeta de notificación de privacidad */
@Composable
private fun PrivacyNoticeCard(
    title: String,
    message: String,
    icon: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(14.dp))
            .border(1.dp, Line.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Text1,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

/** Tarjeta de estado vacío */
@Composable
private fun EmptyStateCard(
    title: String,
    message: String,
    icon: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Card2, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 28.sp)
        }
        Text(
            title,
            color = Text1,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            message,
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/** Tarjeta individual de partida del historial */
@Composable
private fun HistoryGameCard(
    game: RemoteGame,
    showPgn: Boolean,
    showReplay: Boolean,
    onReplay: () -> Unit,
    onTogglePgn: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val resultColor = when {
        game.won == true -> Green
        game.won == false -> Red
        else -> Gold
    }
    val resultText = when {
        game.won == true -> "Victoria"
        game.won == false -> "Derrota"
        else -> "Empate"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        game.dateLabel(),
                        color = Text1,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        "${game.moves} jugadas · ${game.timeLabel.ifBlank { "—" }}",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(resultColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, resultColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            resultText.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = resultColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (showReplay) {
                        TextButton(
                            onClick = { onReplay() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "▶",
                                color = Gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
