/*
 * B-Chess · © Elal Chico
 * Conexiones: centro de la sesión activa (BT / P2P / IP).
 * - Con conexión: ver el perfil del rival, seguir configurando la partida
 *   (Sala) o volver a ella si está en curso. Cerrar esta pantalla NO cierra
 *   la conexión.
 * - Sin conexión: botones grandes con iconos, secciones claras y torneos.
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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

// ===== Paleta minimalista =====
private val ScreenBg = Color(0xFF0F1416)
private val Card = Color(0xFF161C1F)
private val CardDeep = Color(0xFF121719)
private val Line = Color(0xFF263134)
private val LineSoft = Color(0xFF1E272A)
private val ScreenText = Color(0xFFF1F4F2)
private val ScreenMuted = Color(0xFF95A29D)
private val Muted2 = Color(0xFF6C7A76)
private val Green = Color(0xFF5FD3A0)
private val Blue = Color(0xFF6FA8FF)
private val Gold = Color(0xFFC9B15A)
private val Danger = Color(0xFFFF7A7A)
private val OnAccent = Color(0xFF0C1411)

private val CardShape = RoundedCornerShape(18.dp)

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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("← Volver al inicio", color = ScreenMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // ===== Cabecera =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Conexiones", color = ScreenText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (connected) "Sesión en curso" else "Elige cómo jugar",
                    color = ScreenMuted, fontSize = 12.sp
                )
            }
            Spacer(Modifier.weight(1f))
            StatusPill(connected)
        }

        if (connected) {
            ConnectedContent(
                rivalName = rivalName,
                remoteProfile = remoteProfile,
                networkModeName = networkModeName,
                isHost = isHost,
                gameInProgress = gameInProgress,
                onViewRivalProfile = onViewRivalProfile,
                onConfigureMatch = onConfigureMatch,
                onBackToGame = onBackToGame,
                onFinishConnection = onFinishConnection
            )
        } else {
            // ===== BLUETOOTH =====
            SectionLabel("BLUETOOTH")

            // ===== Acción principal: crear partida =====
            BigAction(
                icon = Icons.Outlined.AddCircle,
                title = "Crear sala",
                subtitle = "Bluetooth · sé anfitrión y comparte la conexión",
                accent = Green,
                filled = true,
                onClick = onCreateMatch
            )

            // ===== Acción secundaria: unirse =====
            BigAction(
                icon = Icons.Outlined.Groups,
                title = "Unirme a una sala",
                subtitle = "Bluetooth · busca al anfitrión cercano",
                accent = ScreenText,
                filled = false,
                onClick = onSearchMatch
            )

            // ===== Otros transportes =====
            SectionLabel("OTROS TRANSPORTES")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                color = Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Column {
                    ListRow(
                        icon = Icons.Outlined.Link,
                        accent = Blue,
                        title = "Peer-to-Peer · Internet",
                        subtitle = "Juega a distancia con un código de sala",
                        onClick = onPlayOnline
                    )
                    HorizontalDivider(color = LineSoft)
                    ListRow(
                        icon = Icons.Outlined.Computer,
                        accent = Gold,
                        title = "PC · Red local",
                        subtitle = "Cross-play con B-Chess Desktop (IP:puerto)",
                        onClick = onPlayLan
                    )
                }
            }

            // ===== Torneos =====
            SectionLabel("TORNEOS")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                color = Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Column {
                    ListRow(
                        icon = Icons.Outlined.EmojiEvents,
                        accent = Gold,
                        title = "Crear torneo",
                        subtitle = "Elige Bluetooth, P2P o red local",
                        onClick = { showTournamentDialog = true }
                    )
                    HorizontalDivider(color = LineSoft)
                    ListRow(
                        icon = Icons.Outlined.History,
                        accent = ScreenMuted,
                        title = "Torneos guardados",
                        subtitle = "Historial y resultados de tus series",
                        onClick = onTournamentHistory
                    )
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

/** Contenido cuando hay una conexión activa. */
@Composable
private fun ConnectedContent(
    rivalName: String,
    remoteProfile: RemoteProfile?,
    networkModeName: String,
    isHost: Boolean,
    gameInProgress: Boolean,
    onViewRivalProfile: () -> Unit,
    onConfigureMatch: () -> Unit,
    onBackToGame: () -> Unit,
    onFinishConnection: () -> Unit
) {
    // Estado del transporte
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (isHost) "Anfitrión" else "Invitado",
            color = ScreenMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold
        )
        Text("·", color = Muted2)
        Text(networkModeName, color = Green, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }

    // Perfil del rival (botón grande)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewRivalProfile),
        shape = CardShape,
        color = Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            RemoteAvatarMini(remoteProfile?.photoBase64, rivalName.ifBlank { "R" })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Ver perfil de $rivalName",
                    color = ScreenText, fontWeight = FontWeight.Bold, fontSize = 14.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (remoteProfile != null) {
                    Text(
                        "${remoteProfile.total} jugadas · ${remoteProfile.wins}V · ${remoteProfile.losses}D · ${remoteProfile.draws}E",
                        color = ScreenMuted, fontSize = 11.sp
                    )
                } else {
                    Text("Sincronizando perfil…", color = ScreenMuted, fontSize = 11.sp)
                }
            }
            ChevronGreen()
        }
    }

    // Acción principal
    BigAction(
        icon = if (gameInProgress) Icons.Outlined.EmojiEvents else Icons.Outlined.AddCircle,
        title = if (gameInProgress) "Volver a la partida" else "Configurar partida",
        subtitle = if (gameInProgress) "La partida sigue en curso"
        else if (isHost) "Elige modo y reloj; el rival te espera"
        else "Espera a que el anfitrión inicie la partida",
        accent = Green,
        filled = true,
        onClick = if (gameInProgress) onBackToGame else onConfigureMatch
    )

    Text(
        "Cerrar esta pantalla no cierra la conexión.",
        color = Muted2, fontSize = 11.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedButton(
        onClick = onFinishConnection,
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger, containerColor = Color(0xFF201416))
    ) {
        Text("Finalizar conexión", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}

/** Botón grande de acción principal/secundaria con icono y descripción. */
@Composable
private fun BigAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    filled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(onClick = onClick),
        shape = CardShape,
        color = if (filled) Color(0xFF12211B) else CardDeep,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (filled) Green.copy(alpha = 0.6f) else Line
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconTile(icon = icon, accent = if (filled) Green else ScreenMuted, big = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = if (filled) Green else ScreenText,
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(subtitle, color = ScreenMuted, fontSize = 11.5.sp, maxLines = 2)
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (filled) Green else Muted2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Fila de lista: icono, título, descripción y chevron. */
@Composable
private fun ListRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconTile(icon = icon, accent = accent, big = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = ScreenText, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ScreenMuted, fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Muted2,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Cuadro de icono con fondo tintado. */
@Composable
private fun IconTile(icon: ImageVector, accent: Color, big: Boolean) {
    val size = if (big) 46.dp else 40.dp
    Box(
        Modifier.size(size).clip(RoundedCornerShape(if (big) 14.dp else 12.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(if (big) 24.dp else 21.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Muted2,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun StatusPill(connected: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (connected) Color(0xFF12211B) else Card,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (connected) Green.copy(alpha = 0.45f) else Line
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(7.dp).background(if (connected) Green else Muted2, CircleShape))
            Text(
                if (connected) "Conectado" else "Sin conexión",
                color = if (connected) Green else ScreenMuted,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChevronGreen() {
    Icon(
        Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = Green,
        modifier = Modifier.size(20.dp)
    )
}

/** Selector del transporte para crear un torneo. */
@Composable
private fun TournamentConnectionDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Crear torneo", color = ScreenText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Elige el tipo de conexión. Configurarás las partidas en la sala.",
                        color = ScreenMuted, fontSize = 11.5.sp, lineHeight = 16.sp
                    )
                }
                ChoiceRow(Icons.Outlined.AddCircle, Green, "Bluetooth", "Dispositivos cercanos") { onSelect("bt") }
                ChoiceRow(Icons.Outlined.Link, Blue, "Peer-to-Peer", "Internet con código de sala") { onSelect("p2p") }
                ChoiceRow(Icons.Outlined.Computer, Gold, "Red local / PC", "IP:puerto") { onSelect("lan") }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancelar", color = ScreenMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(CardDeep)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        IconTile(icon = icon, accent = accent, big = false)
        Column(Modifier.weight(1f)) {
            Text(title, color = ScreenText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ScreenMuted, fontSize = 11.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = accent, modifier = Modifier.size(18.dp))
    }
}
