/*
 * B-Chess · © Elal Chico
 * Conexiones: centro de la sesión activa (BT / P2P / IP).
 * - Con conexión: ver el perfil del rival, seguir configurando la partida
 *   (Sala) o volver a ella si está en curso. Cerrar esta pantalla NO cierra
 *   la conexión.
 * - Sin conexión: accesos para crear/unirse por Bluetooth o Internet.
 */

package com.bchess.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onBack: () -> Unit
) {
    val rivalName = remoteProfile?.name?.ifBlank { remoteName.ifBlank { "Rival" } }
        ?: remoteName.ifBlank { "Rival" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Volver
        androidx.compose.material3.TextButton(
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
                        color = if (connected) Color(0xFF9BE3BD) else ScreenMuted,
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

            // Configurar la partida / volver a ella
            if (gameInProgress) {
                Button(
                    onClick = onBackToGame,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenAccent, contentColor = Color.White)
                ) {
                    Text("Volver a la partida", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            } else {
                Button(
                    onClick = onConfigureMatch,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenAccent, contentColor = Color.White)
                ) {
                    Text("Configurar partida", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            Text(
                if (isHost) "El rival está conectado · la partida comienza cuando ambas partes elijan modo y reloj."
                else "Conectado con el anfitrión · espera su inicio de partida.",
                color = ScreenMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.weight(1f))

            // Finalizar conexión
            OutlinedButton(
                onClick = onFinishConnection,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenError.copy(alpha = 0.5f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = ScreenError)
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
            // Sin conexión: accesos rápidos
            Text(
                "No hay ninguna partida conectada en este momento.",
                color = ScreenText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Crea una sala y comparte el código o espera a que el rival se una. Al conectar aparecerá aquí su perfil y la configuración de la partida.",
                color = ScreenMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Button(
                onClick = onCreateMatch,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenAccent, contentColor = Color.White)
            ) {
                Text("Crear partida · Bluetooth", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            OutlinedButton(
                onClick = onSearchMatch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = ScreenText)
            ) {
                Text("Unirme a partida · Bluetooth", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onPlayOnline,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6FA8FF))
            ) {
                Text("Partida Peer-to-Peer · Internet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onPlayLan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = ScreenText)
            ) {
                Text("Unirse a PC · Red local (IP:puerto)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BoxStatusDot(connected: Boolean) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .background(if (connected) Color(0xFF3DDC97) else ScreenMuted, CircleShape)
    )
}
