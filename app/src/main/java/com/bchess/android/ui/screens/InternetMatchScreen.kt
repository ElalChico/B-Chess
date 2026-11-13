package com.bchess.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.data.BtState

private val ScreenBg = Color(0xFF101416)
private val SurfaceColor = Color(0xFF1A2021)
private val SurfaceMuted = Color(0xFF202726)
private val ScreenText = Color(0xFFF0F3F1)
private val ScreenMuted = Color(0xFFAAB5B1)
private val ScreenAccent = Color(0xFF789889)
private val ScreenLine = Color(0xFF303A39)
private val ScreenError = Color(0xFFD98B86)
private val ScreenGold = Color(0xFFC9B15A)

/**
 * Internet match flow:
 * - "Crear partida" → genera un código (matchId) que el invitado ingresa
 * - "Unirme con código" → el invitado pega el código del anfitrión
 *
 * Una vez conectado, el juego pasa a la sala (RoomScreen) igual que en Bluetooth,
 * y el protocolo de mensajes es idéntico (BtMessage JSON).
 */
@Composable
fun InternetMatchScreen(
    state: BtState,
    matchId: String?,
    playerName: String,
    error: String?,
    onCreateMatch: () -> Unit,
    onJoinMatch: (String) -> Unit,
    onBack: () -> Unit,
    onFinishConnection: () -> Unit = {}
) {
    var mode by remember { mutableStateOf<String?>(null) } // null | "create" | "join"
    var joinCode by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var showConnectionSuccess by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val waiting = state == BtState.WAITING_HOST
    val connecting = state == BtState.CONNECTING
    val connected = state == BtState.CONNECTED

    if (copied) {
        LaunchedEffect(copied) {
            kotlinx.coroutines.delay(2200)
            copied = false
        }
    }

    // Mostrar mensaje de éxito de conexión cuando se conecta
    LaunchedEffect(connected) {
        if (connected) {
            showConnectionSuccess = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onBack,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ScreenText)
            ) {
                Text("Volver", fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("PEER-TO-PEER", color = ScreenMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Jugar Peer-to-Peer", color = ScreenText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Conexión directa dispositivo a dispositivo · $playerName", color = ScreenMuted, fontSize = 13.sp)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!waiting && !connecting) {
                // ---- Elegir rol ----
                Button(
                    onClick = { mode = "create"; onCreateMatch() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenGold, contentColor = Color(0xFF17130A))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Crear partida", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text("Genera un código y compártelo", fontSize = 12.sp, color = Color(0xFF3A2E14))
                    }
                }

                OutlinedButton(
                    onClick = { mode = "join" },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ScreenText)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Unirme con código", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Pega el código del anfitrión", fontSize = 12.sp, color = ScreenMuted)
                    }
                }
            }

            // ---- Vista de creación (anfitrión) ----
            if (mode == "create" || waiting) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (matchId == null) {
                            CircularProgressIndicator(color = ScreenAccent, modifier = Modifier.size(28.dp))
                            Text("Generando partida…", color = ScreenMuted, fontSize = 13.sp)
                        } else {
                            Text("CÓDIGO DE PARTIDA", color = ScreenMuted, fontSize = 11.sp, letterSpacing = 2.sp)
                            Text(
                                matchId,
                                color = ScreenGold,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.5.sp
                            )
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(matchId!!))
                                    copied = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (copied) ScreenAccent else ScreenLine),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (copied) ScreenAccent else ScreenText),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (copied) "¡Copiada!" else "Copiar código",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            if (waiting) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(color = ScreenAccent, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Esperando al invitado…", color = ScreenMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ---- Vista de unión (invitado) ----
            if (mode == "join" || connecting) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("CÓDIGO DEL ANFITRIÓN", color = ScreenMuted, fontSize = 11.sp, letterSpacing = 2.sp)
                        Text("Pega aquí el código completo que te envió el anfitrión", color = ScreenMuted, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.trim() },
                                modifier = Modifier.weight(1f),
                                singleLine = false,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScreenAccent,
                                    unfocusedBorderColor = ScreenLine,
                                    focusedTextColor = ScreenText,
                                    unfocusedTextColor = ScreenText
                                ),
                                placeholder = {
                                    Text("Pega el código del anfitrión aquí...", color = ScreenMuted.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            )
                            TextButton(
                                onClick = {
                                    clipboard.getText?.let { joinCode = it.trimToEmpty() }
                                },
                                enabled = joinCode.isNotBlank()
                            ) {
                                Text("Pegar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Button(
                            onClick = { onJoinMatch(joinCode) },
                            enabled = joinCode.length >= 10 && !connecting,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScreenGold, contentColor = Color(0xFF17130A)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (connecting) {
                                    CircularProgressIndicator(color = Color(0xFF17130A), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Conectando…", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Conectar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            error?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScreenError.copy(alpha = 0.4f))
                ) {
                    Text(it, color = ScreenError, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(14.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceMuted
            ) {
                Text(
                    "Los movimientos viajan directamente de un teléfono a otro por una conexión entre pares (P2P), sin intermediarios ni servidores centrales durante la partida. Solo el código inicial se comparte mediante la red de emparejamiento.",
                    color = ScreenMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    lineHeight = 17.sp
                )
            }

            // ==== MENSAJE DE ÉXITO DE CONEXIÓN ====
            if (connected && showConnectionSuccess) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF14241D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32.copy(alpha = 0.6f)))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF4CAF50.copy(alpha = 0.2f)), CircleShape)
                                .border(2.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            "¡Conexión exitosa!",
                            color = ScreenText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Estás conectado con tu rival. Los movimientos viajarán directamente de dispositivo a dispositivo.",
                            color = ScreenMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Button(
                            onClick = { showConnectionSuccess = false },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScreenAccent, contentColor = Color.White)
                        ) {
                            Text("Entendido", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ==== BOTÓN DE FINALIZAR CONEXIÓN ====
            if (connected) {
                Button(
                    onClick = onFinishConnection,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Box(Modifier.size(16.dp).background(Color(0xFFFFA726), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Finalizar conexión", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
