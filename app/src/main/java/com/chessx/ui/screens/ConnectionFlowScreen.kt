package com.chessx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.chessx.data.BtState
import com.chessx.data.DeviceRole
import com.chessx.data.DiscoveredDevice

private val ScreenBg = Color(0xFF101416)
private val SurfaceColor = Color(0xFF1A2021)
private val SurfaceMuted = Color(0xFF202726)
private val ScreenText = Color(0xFFF0F3F1)
private val ScreenMuted = Color(0xFFAAB5B1)
private val ScreenAccent = Color(0xFF789889)
private val ScreenLine = Color(0xFF303A39)
private val ScreenError = Color(0xFFD98B86)

@Composable
fun ConnectionFlowScreen(
    isHost: Boolean,
    state: BtState,
    devices: List<DiscoveredDevice>,
    playerName: String,
    remoteName: String,
    error: String?,
    onListen: () -> Unit,
    onSearch: () -> Unit,
    onConnect: (String) -> Unit,
    onBack: () -> Unit
) {
    val appDevices = devices.filter { it.isChessX }.sortedByDescending { it.rssi }
    val otherDevices = devices.filterNot { it.isChessX }.sortedByDescending { it.rssi }
    val busy = state == BtState.CONNECTING
    val active = if (isHost) state == BtState.WAITING_HOST else state == BtState.SCANNING

    Column(
        modifier = Modifier.fillMaxSize().background(ScreenBg).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ScreenText)
            ) {
                Text("Volver", fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("PASO 2 DE 3", color = ScreenMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Conectar por Bluetooth", color = ScreenText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isHost) "Anfitrión · $playerName" else "Invitado · $playerName",
                color = ScreenMuted,
                fontSize = 13.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, ScreenLine)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                if (active || busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ScreenAccent)
                } else {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(10.dp).background(
                            if (state == BtState.CONNECTED) ScreenAccent else ScreenMuted,
                            CircleShape
                        )
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when (state) {
                            BtState.CONNECTED -> "Conectado con ${remoteName.ifBlank { "el rival" }}"
                            BtState.WAITING_HOST -> "Esperando al invitado"
                            BtState.SCANNING -> "Buscando anfitriones"
                            BtState.CONNECTING -> "Conectando…"
                            BtState.ERROR -> "No se pudo conectar"
                            BtState.DISCONNECTED -> "Conexión interrumpida"
                            else -> if (isHost) "Listo para recibir una conexión" else "Listo para buscar móviles"
                        },
                        color = ScreenText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when {
                            state == BtState.CONNECTED -> "La configuración de partida abrirá enseguida."
                            isHost -> "El otro jugador debe elegir «Unirse» y seleccionar este móvil."
                            else -> "El anfitrión debe elegir «Crear partida» y mantener esta pantalla abierta."
                        },
                        color = ScreenMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Button(
            onClick = if (isHost) onListen else onSearch,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ScreenAccent,
                contentColor = Color.White,
                disabledContainerColor = SurfaceMuted,
                disabledContentColor = ScreenMuted
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(
                when {
                    busy -> "Conectando…"
                    active && isHost -> "Escuchando conexiones"
                    active -> "Buscando móviles…"
                    isHost -> "Activar modo anfitrión"
                    else -> "Buscar anfitriones"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (error != null) {
            Text(error, color = ScreenError, fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Móviles cercanos", color = ScreenText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${devices.size}", color = ScreenMuted, fontSize = 12.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (appDevices.isEmpty() && otherDevices.isEmpty()) {
                item {
                    Text(
                        if (isHost) "Cuando el invitado busque, su móvil aparecerá aquí."
                        else "Activa la búsqueda para ver móviles cercanos.",
                        color = ScreenMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)
                    )
                }
            }
            if (appDevices.isNotEmpty()) {
                item { Text("CHESS-X", color = ScreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                items(appDevices, key = { it.address }) { device ->
                    NearbyDeviceRow(device, isHost, onConnect)
                }
            }
            if (otherDevices.isNotEmpty()) {
                item { Text("OTROS DISPOSITIVOS", color = ScreenMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                items(otherDevices, key = { it.address }) { device ->
                    NearbyDeviceRow(device, isHost, onConnect)
                }
            }
        }
    }
}

@Composable
private fun NearbyDeviceRow(device: DiscoveredDevice, isHost: Boolean, onConnect: (String) -> Unit) {
    val name = device.username?.takeIf { it.isNotBlank() } ?: device.name
    val role = when (device.role) {
        DeviceRole.HOST -> "Anfitrión"
        DeviceRole.GUEST -> "Invitado"
        DeviceRole.UNKNOWN -> "Bluetooth"
    }
    val highlighted = device.isChessX
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) Color(0xFF24312C) else SurfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (highlighted) Color(0xFF52685C) else ScreenLine)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = ScreenText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (highlighted) "CHESS-X · $role" else "Dispositivo Bluetooth",
                    color = if (highlighted) Color(0xFFB8D0C2) else ScreenMuted,
                    fontSize = 11.sp
                )
            }
            if (!isHost) {
                Button(
                    onClick = { onConnect(device.address) },
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenAccent, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
