package com.bchess.android.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.data.LobbyState
import com.bchess.android.data.PredefinedControls
import com.bchess.android.data.RemoteProfile
import com.bchess.android.R

private val Bg = Color(0xFF101416)
private val Card = Color(0xFF1A2021)
private val Card2 = Color(0xFF202726)
private val Line = Color(0xFF303A39)
private val Line2 = Color(0xFF3B4643)
private val Text1 = Color(0xFFF0F3F1)
private val Muted = Color(0xFFAAB5B1)
private val Gold = Color(0xFF789889)
private val Green = Color(0xFF81B39A)
private val Red = Color(0xFFD98B86)

/**
 * Sala de partida: permite configurar reloj y color, conectar al rival e iniciar.
 */
@Composable
fun RoomScreen(
    lobby: LobbyState,
    localName: String,
    remoteName: String,
    localPhotoBase64: String = "",
    remoteProfile: RemoteProfile? = null,
    onViewRivalProfile: () -> Unit = {},
    onTimePick: (Long, Long) -> Unit,
    onColorPick: (ChessColor) -> Unit,
    onRandomColor: () -> Unit,
    onConnect: () -> Unit,
    onStartGame: () -> Unit,
    onStartTournament: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    val isHost = lobby.isHost
    val rivalOk = remoteName.isNotBlank() && lobby.connected
    val clockLabel = if (lobby.timeMs == 0L) "Sin límite" else "${lobby.timeMs / 60000} + ${lobby.incrementMs / 1000}"
    val colorLabel = if (lobby.colorRandom) "Aleatorio" else if (lobby.localColor == ChessColor.WHITE) "Blancas" else "Negras"
    var tournamentMode by remember { mutableStateOf(false) }
    var tournamentGames by remember { mutableIntStateOf(4) }
    var modeSelected by remember { mutableStateOf(false) }
    var clockSelected by remember { mutableStateOf(false) }
    var showConnectedDialog by remember { mutableStateOf(false) }
    var lastDialogOpponent by remember { mutableStateOf<String?>(null) }
    val roomScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        roomScrollState.scrollTo(0)
    }

    LaunchedEffect(isHost, rivalOk, remoteName) {
        if (isHost && rivalOk && remoteName.isNotBlank() && remoteName != lastDialogOpponent) {
            lastDialogOpponent = remoteName
            showConnectedDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(roomScrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                Text("← Volver al inicio", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF12241B), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3B2B))) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(7.dp).background(Green, CircleShape))
                    Text("SALA", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9BE3BD), letterSpacing = 1.sp)
                }
            }
        }

        // Cabecera: quién es anfitrión y quién invitado
        Text(
        "Paso 3 de 3 · Prepara la partida",
        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Text1
        )
        Text(
        if (isHost) "Elige el modo y el tiempo. Cuando esté listo, comienza."
        else "El anfitrión está configurando la partida.",
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp
        )

        // Ambos jugadores en tarjetas
        RoomPlayerCard(
            name = localName.ifBlank { "Jugador" },
            role = if (isHost) "ANFITRIÓN" else "INVITADO",
            color = lobby.localColor,
            photoBase64 = localPhotoBase64,
            accent = Gold,
            you = true,
            onClick = null
        )
        RoomPlayerCard(
            name = remoteProfile?.name?.ifBlank { remoteName.ifBlank { "Rival" } } ?: remoteName.ifBlank { "Rival" },
            role = if (isHost) "INVITADO" else "ANFITRIÓN",
            color = lobby.remoteColor,
            photoBase64 = remoteProfile?.photoBase64,
            accent = Color(0xFF5B9DF9),
            you = false,
            onClick = { onViewRivalProfile() }
        )

        if (isHost) {
            SectionLabel("1 · MODO DE JUEGO")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(false, true).forEach { isTournament ->
                    val selected = modeSelected && tournamentMode == isTournament
                    Button(
                        onClick = {
                                modeSelected = true
                                tournamentMode = isTournament
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF40574B) else Card2,
                            contentColor = Text1
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (isTournament) "Torneo" else "Individual",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isTournament) "Varias partidas" else "Una partida",
                                color = if (selected) Color.White.copy(alpha = 0.82f) else Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            if (tournamentMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Card, RoundedCornerShape(14.dp))
                        .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Stepper("Partidas", tournamentGames, 2, 20, Modifier.weight(1f)) { tournamentGames = it }
                    Text(
                        "Colores alternos · $tournamentGames partidas",
                        modifier = Modifier.weight(1.5f),
                        fontSize = 11.sp,
                        color = Muted
                    )
                }
            }
        }

        if (isHost) {
            SectionLabel("2 · ELIGE EL TIEMPO")
        Column(
            modifier = Modifier.fillMaxWidth().alpha(if (isHost) 1f else 0.55f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PredefinedControls.options.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { opt ->
                        val selected = clockSelected &&
                            lobby.timeMs == opt.timeMs &&
                            lobby.incrementMs == opt.incrementMs
                        Button(
                            onClick = {
                                clockSelected = true
                                onTimePick(opt.timeMs, opt.incrementMs)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF40574B) else Card2,
                                contentColor = Text1
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(opt.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (opt.timeMs == 0L) "Sin límite" else if (opt.incrementMs > 0) "+${opt.incrementMs / 1000}s" else "${opt.timeMs / 60000} min",
                                    fontSize = 9.sp,
                                    color = if (selected) Color.White.copy(alpha = 0.82f) else Muted
                                )
                            }
                        }
                    }
                    if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (isHost) {
                var cMin by remember { mutableIntStateOf((if (lobby.timeMs > 0) lobby.timeMs / 60000 else 5).toInt().coerceIn(1, 120)) }
                var cInc by remember { mutableIntStateOf((lobby.incrementMs / 1000).toInt().coerceIn(0, 30)) }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Card2, RoundedCornerShape(14.dp)).border(1.dp, Line2, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Stepper("Min", cMin, 1, 120, Modifier.weight(1f)) {
                        cMin = it
                        clockSelected = true
                        onTimePick(cMin * 60_000L, cInc * 1000L)
                    }
                    Stepper("+Seg", cInc, 0, 30, Modifier.weight(1f)) {
                        cInc = it
                        clockSelected = true
                        onTimePick(cMin * 60_000L, cInc * 1000L)
                    }
                }
            }
        }
        }

        if (isHost) {
            SectionLabel("COLOR")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorCard("Blancas", "Mueven primero", ChessColor.WHITE, !lobby.colorRandom && lobby.localColor == ChessColor.WHITE, Modifier.weight(1f)) { onColorPick(ChessColor.WHITE) }
                ColorCard("Negras", "Juegan segundo", ChessColor.BLACK, !lobby.colorRandom && lobby.localColor == ChessColor.BLACK, Modifier.weight(1f)) { onColorPick(ChessColor.BLACK) }
                RandomColorCard("Aleatorio", "Al comenzar", lobby.colorRandom, Modifier.weight(1f), onRandomColor)
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Card2, RoundedCornerShape(12.dp))
                    .border(1.dp, Line2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChecklistItem("Reloj", clockLabel, clockSelected)
                ChecklistItem("Color", colorLabel, true)
                ChecklistItem("Rival", if (rivalOk) remoteName.take(10) else "esperando", rivalOk)
            }
        } else {
            Surface(shape = RoundedCornerShape(14.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Gold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        if (rivalOk) "Conectado · esperando que el anfitrión comience" else "Buscando al anfitrión por Bluetooth…",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        }

        if (isHost) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!rivalOk) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = Gold, modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Escuchando rivales cercanos", color = Text1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Tu sala aparecerá con el nombre de tu perfil", color = Muted, fontSize = 10.sp)
                            }
                            TextButton(onClick = onConnect, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text("Reintentar", color = Gold, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        if (tournamentMode) onStartTournament(tournamentGames) else onStartGame()
                    },
                    enabled = rivalOk && modeSelected && clockSelected,
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF789889),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF29312E),
                        disabledContentColor = Muted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Text(
                        when {
                            !rivalOk -> "Esperando contrincante"
                            !modeSelected -> "Selecciona el modo"
                            !clockSelected -> "Selecciona el reloj"
                            tournamentMode -> "Comenzar torneo  →"
                            else -> "Comenzar partida  →"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    when {
                        !rivalOk -> "El botón se activará cuando el rival esté conectado."
                        !modeSelected -> "Elige Individual o Torneo para continuar."
                        !clockSelected -> "Elige un reloj para continuar."
                        else -> "Ambos móviles iniciarán con una cuenta atrás sincronizada."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = Muted,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("Salir de la sala", color = Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showConnectedDialog) {
        val opponentName = remoteProfile?.name?.ifBlank { remoteName } ?: remoteName
        AlertDialog(
            onDismissRequest = { showConnectedDialog = false },
            title = { Text("$opponentName conectado", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("El perfil del rival está listo.")
                    Text(
                        if (modeSelected && clockSelected) {
                            if (tournamentMode) "Torneo de $tournamentGames partidas · $clockLabel"
                            else "Partida individual · $clockLabel"
                        } else {
                            "Completa el tipo de partida y el reloj antes de iniciar."
                        },
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (modeSelected && clockSelected) {
                            showConnectedDialog = false
                            if (tournamentMode) onStartTournament(tournamentGames) else onStartGame()
                        } else {
                            showConnectedDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF131313))
                ) {
                    Text(
                        if (modeSelected && clockSelected) "Comenzar partida" else "Configurar partida",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectedDialog = false }) {
                    Text("Seguir configurando")
                }
            },
            containerColor = Card
        )
    }
}

@Composable
private fun RoomPlayerCard(
    name: String,
    role: String,
    color: ChessColor,
    photoBase64: String?,
    accent: Color,
    you: Boolean,
    onClick: (() -> Unit)?
) {
    val bmp = remember(photoBase64) {
        try {
            photoBase64?.substringAfter("base64,", photoBase64)?.let { raw ->
                val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }
    val badge = when {
        color == ChessColor.WHITE -> "B"
        else -> "N"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A2433))
                .border(1.5.dp, accent.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text((name.firstOrNull() ?: '?').uppercaseChar().toString(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Text1, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Text(if (you) "Tú · $role" else "$role · rival", color = Muted, fontSize = 10.5.sp)
        }
        // Insignia de color (peoncito) — Lite
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (color == ChessColor.WHITE) Color(0xFFECECF4) else Color(0xFF141414), RoundedCornerShape(9.dp))
                .border(1.dp, Line2, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(badge, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (color == ChessColor.WHITE) Color(0xFF141414) else Color(0xFFECECF4))
        }
    }
}

@Composable
private fun ChecklistItem(label: String, value: String, ok: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (ok) Green else Color(0xFF2A2A2A), CircleShape)
            )
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Text(value, color = if (ok) Text1 else Muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun ColorCard(
    title: String,
    sub: String,
    isWhite: ChessColor,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val white = isWhite == ChessColor.WHITE
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(16.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF0E1318), RoundedCornerShape(13.dp))
                .border(1.dp, Line, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = if (white) R.drawable.piece_wp else R.drawable.piece_bp),
                contentDescription = title,
                modifier = Modifier.size(44.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Gold else Text1)
            Text(sub, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun RandomColorCard(
    title: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF26200F) else Card2, RoundedCornerShape(16.dp))
            .border(1.dp, if (selected) Gold else Line2, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFF0E1318), RoundedCornerShape(13.dp))
                .border(1.dp, Line, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Gold else Text1)
            Text(sub, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallBtn("−") { if (value > min) onChange(value - 1) }
            Text("$value", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
            SmallBtn("+") { if (value < max) onChange(value + 1) }
        }
    }
}

@Composable
private fun SmallBtn(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).background(Color(0xFF10151C), RoundedCornerShape(8.dp)).border(1.dp, Line2, RoundedCornerShape(8.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = Text1, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}
