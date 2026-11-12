package com.chessx.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val Bg = Color(0xFF101416)
private val Card = Color(0xFF1A2021)
private val Line = Color(0xFF303A39)
private val Text1 = Color(0xFFF0F3F1)
private val Muted = Color(0xFFAAB5B1)
private val Accent = Color(0xFF789889)
private val Green = Color(0xFF81B39A)

@Composable
fun HomeScreen(
    inSession: Boolean,
    isGuestSession: Boolean,
    profileConfigured: Boolean,
    profileName: String,
    profilePhotoBase64: String,
    clockSummary: String,
    onProfile: () -> Unit,
    onCreate: () -> Unit,
    onSearch: () -> Unit,
    onPractice: () -> Unit
) {
    val avatar = remember(profilePhotoBase64) {
        try {
            profilePhotoBase64.substringAfter("base64,", profilePhotoBase64).takeIf { it.isNotBlank() }?.let {
                val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Image(
                    painter = painterResource(com.bchess.android.R.drawable.chessx_mark),
                    contentDescription = "Logo de B-Chess",
                    modifier = Modifier.size(42.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("B-Chess", color = Text1, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Text("Ajedrez local y práctica con Stockfish.", color = Muted, fontSize = 12.sp)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (inSession) Color(0xFF24332D) else Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.size(7.dp).background(if (inSession) Green else Muted, CircleShape))
                    Text(
                        if (inSession) "Conectado" else "Bluetooth",
                        color = if (inSession) Green else Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onProfile),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121922),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF273341))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1A2430), CircleShape)
                        .border(1.dp, Accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null) {
                        Image(avatar, contentDescription = "Foto de $profileName", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(profileName.firstOrNull()?.uppercaseChar()?.toString() ?: "J", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(profileName.ifBlank { "Completa tu perfil" }, color = Text1, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        if (profileConfigured) "Perfil e historial" else "Agrega tu nombre y foto para empezar",
                        color = if (profileConfigured) Muted else Accent,
                        fontSize = 12.sp
                    )
                }
                Text("›", color = Accent, fontSize = 27.sp, fontWeight = FontWeight.Normal)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("SIGUIENTE PASO", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text(
                if (profileConfigured) "Conecta por Bluetooth" else "Completa tu perfil para empezar",
                color = Text1,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeAction(
                title = "Crear partida",
                subtitle = "Ser anfitrión",
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = onCreate
            )
            HomeAction(
                title = "Unirse",
                subtitle = "Buscar anfitrión",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onSearch
            )
        }

        OutlinedButton(
            onClick = onPractice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text("Practicar contra Stockfish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Partida local", color = Muted, fontSize = 11.sp)
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = Card, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (inSession) "Sala conectada" else "Tu reloj habitual", color = Text1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            inSession && isGuestSession -> "Esperando el inicio del anfitrión"
                            inSession -> "Listos para comenzar"
                            else -> "Individual o torneo · el rival se conecta cerca"
                        },
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
                Text(if (inSession) "●" else clockSummary, color = if (inSession) Green else Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

}

@Composable
private fun HomeAction(
    title: String,
    subtitle: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Accent else Card,
            contentColor = Text1
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = if (primary) Color.White.copy(alpha = 0.85f) else Muted, fontSize = 11.sp)
        }
    }
}
