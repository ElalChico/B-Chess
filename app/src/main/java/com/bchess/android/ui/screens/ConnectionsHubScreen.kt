/*
 * B-Chess · © Elal Chico
 * Conexiones: centro de la sesión activa (BT / P2P / IP).
 * - Con conexión: ver el perfil del rival, seguir configurando la partida
 *   (Sala) o volver a ella si está en curso. Cerrar esta pantalla NO cierra
 *   la conexión.
 * - Sin conexión: accesos diferenciados por transporte y torneos.
 */

package com.bchess.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bchess.android.data.RemoteProfile

private val ScreenBg = Color(0xFF101416)
private val SurfaceColor = Color(0xFF1A2021)
private val SurfaceGreen = Color(0xFF14241D)
private val ScreenText = Color(0xFFF0F3F1)
private val ScreenMuted = Color(0xFFAAB5B1)
private val ScreenAccent = Color(0xFF789889)
private val ScreenLine = Color(0xFF303A39)
private val ScreenGold = Color(0xFFC9B15A)
private val ScreenError = Color(0xFFD98B86)
private val Blue = Color(0xFF6FA8FF)
private val Green = Color(0xFF5FD3A0)

private val CardShape = RoundedCornerShape(18.dp)
private val CardColor = Color(0xFF101416)
private val Divider2 = Color(0xFF4A4A4A)

@Composable
fun ConnectionsHubScreen(
    connected: Boolean,
    networkModeName: String,
    isHost: Boolean,
    localName: String,
    remoteName: String,
    remoteProfile: RemoteProfile?,
    gameInProgress: Boolean,
    onViewRivalProfile: () -> Unit,
    onConfigureMatch: () -> Unit,
    onBackToGame: () -> Unit,
    onFinishConnection: () -> Unit,
    onCreateMatch: () -> Unit,
    onSearchMatch: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayLan: () -> Unit,
    onTournamentHistory: () -> Unit,
    onStartTournament: (String) -> Unit,
    onBack: () -> Unit
) {
    val rivalName = remoteProfile?.name?.ifBlank { remoteName.ifBlank { "Rival" } }
        ?: remoteName.ifBlank { "Rival" }
    var showTournamentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text("← Volver al inicio", color = ScreenMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Título + estado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Conexiones", color = ScreenText, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (connected) SurfaceGreen else Color(0xFF1A2021),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (connected) Color(0xFF2E5B3E) else ScreenLine
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BoxStatusDot(connected)
                    Text(
                        if (connected) "CONECTADO" else "SIN CONEXIÓN",
                        color = if (connected) Green else ScreenMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        if (connected) {
            // Transporte activo
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (isHost) "Eres anfitrión" else "Eres invitado",
                        color = ScreenMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("·", color = ScreenMuted)
                    Text(
                        networkModeName,
                        color = ScreenAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Ver perfil del rival (tarjeta tocable)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF151C25),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3524))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewRivalProfile() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RemoteAvatarMini(remoteProfile?.photoBase64, rivalName.ifBlank { "R" })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Ver perfil de $rivalName",
                            color = ScreenGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (remoteProfile != null) {
                            Text(
                                "${remoteProfile.total} jugadas · ${remoteProfile.wins}V · ${remoteProfile.losses}D · ${remoteProfile.draws}E",
                                color = ScreenMuted,
                                fontSize = 11.sp
                            )
                        } else {
                            Text("Perfil del rival disponible al sincronizar", color = ScreenMuted, fontSize = 11.sp)
                        }
                    }
                    Text("→", color = ScreenGold, fontWeight = FontWeight.ExtraBold)
                }
            }

            Button(
                onClick = if (gameInProgress) onBackToGame else onConfigureMatch,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF0C1411))
            ) {
                Text(
                    if (gameInProgress) "Volver a la partida" else "Configurar partida",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Text(
                if (isHost) "El rival está conectado · la partida comienza cuando ambas partes elijan modo y reloj."
                else "Conectado con el anfitrión · espera su inicio de partida.",
                color = ScreenMuted,
                fontSize = 11.sp
            )

            OutlinedButton(
                onClick = onFinishConnection,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenError.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ScreenError)
            ) {
                Text("Finalizar conexión", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(
                "Cerrar esta pantalla no cierra la conexión.",
                color = ScreenMuted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            // Bloque "Crear sala / Unirme" en una tarjeta vertical
            Surface(
                shape = CardShape,
                color = CardColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider2)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TouchableOpacity(onClick = onCreateMatch) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Green.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.AddCircle, null, tint = Green, modifier = Modifier.size(20.dp))
                            }
                        }
                        TouchableOpacity(onClick = onSearchMatch) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Color(0xFF232E2A).copy(alpha = 0.6f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Groups, null, tint = Color(0xFF8CA08B), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Separador
            Surface(
                shape = CardShape,
                color = CardColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }

            TouchableOpacity(onClick = onPlayOnline) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Blue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Link, null, tint = Blue, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "Peer-to-Peer",
                        color = ScreenText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            TouchableOpacity(onClick = onPlayLan) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ScreenGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Computer, null, tint = ScreenGold, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "Red local/PC",
                        color = ScreenGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Separador
            Surface(
                shape = CardShape,
                color = CardColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }

            // ===== TORNEOS =====
            Text(
                "TORNEOS",
                color = ScreenMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                color = SurfaceColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine)
            ) {
                Column {
                    TournamentRow(
                        icon = Icons.Outlined.History,
                        accent = ScreenAccent,
                        title = "Torneos guardados",
                        subtitle = "Historial y resultados de tus series"
                    ) { onTournamentHistory() }
                    androidx.compose.material3.HorizontalDivider(color = ScreenLine)
                    TournamentRow(
                        icon = Icons.Outlined.EmojiEvents,
                        accent = ScreenGold,
                        title = "Crear torneo",
                        subtitle = "Elige Bluetooth, P2P o red local"
                    ) { showTournamentDialog = true }
                }
            }
        }
    }

    if (showTournamentDialog) {
        TournamentConnectionDialog(
            onSelect = { kind ->
                showTournamentDialog = false
                onStartTournament(kind)
            },
            onDismiss = { showTournamentDialog = false }
        )
    }
}

/** Wrapper simplificado para TouchableOpacity. */
@Composable
private fun TouchableOpacity(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TournamentRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = ScreenText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ScreenMuted, fontSize = 10.5.sp)
        }
        Text("›", color = accent, fontSize = 20.sp)
    }
}

/** Selector del transporte para crear un torneo. */
@Composable
private fun TournamentConnectionDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Crear torneo", color = ScreenText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Elige el tipo de conexión. Luego configura las partidas en la sala.",
                    color = ScreenMuted, fontSize = 11.5.sp
                )
                TournamentChoice(Icons.Outlined.AddCircle, Green, "Bluetooth", "Dispositivos cercanos") { onSelect("bt") }
                TournamentChoice(Icons.Outlined.Link, Blue, "Peer-to-Peer", "Internet con código de sala") { onSelect("p2p") }
                TournamentChoice(Icons.Outlined.Computer, ScreenGold, "Red local / PC", "IP:puerto") { onSelect("lan") }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancelar", color = ScreenMuted)
                }
            }
        }
    }
}

@Composable
private fun TournamentChoice(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ScreenText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ScreenMuted, fontSize = 10.5.sp)
        }
        Text("→", color = accent, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BoxStatusDot(connected: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(if (connected) Color(0xFF3DDC97) else ScreenMuted, CircleShape)
    )
}