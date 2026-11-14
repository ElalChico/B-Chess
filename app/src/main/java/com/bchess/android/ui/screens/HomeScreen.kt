package com.bchess.android.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bchess.android.data.GitHubProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val Bg = Color(0xFF101416)
private val Card = Color(0xFF1A2021)
private val Line = Color(0xFF303A39)
private val Text1 = Color(0xFFF0F3F1)
private val Muted = Color(0xFFAAB5B1)
private val Accent = Color(0xFF789889)
private val Green = Color(0xFF81B39A)
private val Gold = Color(0xFFC9B15A)
private val PanelPrimary = Color(0xFF28303F)
private val P2PBorder = Color(0xFF344258)
private val P2PText = Color(0xFFD8E2F0)

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
    onPractice: () -> Unit,
    onPlayOnline: () -> Unit = {},
    onTournament: () -> Unit = {},
    onFinishConnection: () -> Unit = {},
    versionUpToDate: Boolean = true
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

    // Avatar de GitHub: se sincroniza siempre con el perfil publico
    val githubAvatar = rememberGitHubAvatar()
    val context = LocalContext.current
    var showAboutDeveloper by remember { mutableStateOf(false) }
    val openUrl: (String) -> Unit = { url ->
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
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
                    // Solo la foto que configuro el usuario. Nunca se muestra el
                    // avatar de GitHub del desarrollador aqui (solo en su tarjeta).
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
            onClick = onTournament,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Gold),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Torneo", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Partidas múltiples · gana la serie", color = Muted, fontSize = 11.sp)
                }
                Text("»", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        OutlinedButton(
            onClick = onPractice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(com.bchess.android.R.drawable.ic_stockfish),
                    contentDescription = "Stockfish engine",
                    tint = Text1,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Practicar contra Stockfish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Motor local con fuerza ajustable", color = Muted, fontSize = 11.sp)
                }
            }
        }

        OutlinedButton(
            onClick = onPlayOnline,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, P2PBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = P2PText),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(com.bchess.android.R.drawable.ic_p2p),
                    contentDescription = "Partida Peer-to-Peer",
                    tint = P2PText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Partida Peer-to-Peer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Protocolo directo dispositivo a dispositivo · sin intermediarios", color = Muted, fontSize = 11.sp)
                }
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

        // ==== BOTÓN DE FINALIZAR CONEXIÓN (solo cuando hay sesión activa) ====
        if (inSession) {
            Button(
                onClick = onFinishConnection,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(Modifier.size(16.dp).background(Color(0xFFFFA726), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Finalizar conexión", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ==== BADGE DE VERSIÓN (solo en Home y Configuraciones) ====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(if (versionUpToDate) Color(0xFF14241D) else Color(0xFF241414), CircleShape)
                    .border(1.dp, if (versionUpToDate) Color(0xFF4CAF50) else Color(0xFFE53935), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (versionUpToDate) "✓" else "✗",
                    color = if (versionUpToDate) Color(0xFF4CAF50) else Color(0xFFE53935),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                if (versionUpToDate) "Versión al día" else "Actualización disponible",
                color = if (versionUpToDate) Color(0xFF4CAF50) else Color(0xFFE53935),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        // ============ FIRMA DEL DESARROLLADOR ============
        DeveloperCard(
            githubAvatar = githubAvatar,
            onAbout = { showAboutDeveloper = true },
            onProfile = { openUrl(GitHubProfile.PROFILE_URL) }
        )
    }

    if (showAboutDeveloper) {
        AboutDeveloperDialog(
            githubAvatar = githubAvatar,
            onProfile = { openUrl(GitHubProfile.PROFILE_URL) },
            onDismiss = { showAboutDeveloper = false }
        )
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
            containerColor = if (primary) PanelPrimary else Card,
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

/**
 * Descarga el avatar publico de GitHub del desarrollador.
 * Se sincroniza siempre (sin cache en el repo) para que la app y el perfil
 * de GitHub muestren la misma imagen.
 */
@Composable
private fun rememberGitHubAvatar(): ImageBitmap? {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(GitHubProfile.AVATAR_URL).build()
                OkHttpClient().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        null
                    } else {
                        val bytes = response.body?.bytes()
                        if (bytes == null) null
                        else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }
    return bitmap
}

/** Tarjeta con la firma del desarrollador y enlaces a GitHub. */
@Composable
private fun DeveloperCard(
    githubAvatar: ImageBitmap?,
    onAbout: () -> Unit,
    onProfile: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAbout)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2430), CircleShape)
                    .border(1.dp, Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (githubAvatar != null) {
                    Image(
                        githubAvatar,
                        contentDescription = "Foto de GitHub de Elal Chico",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("EC", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Creado por Elal Chico", color = Text1, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Desarrollador autodidacta · Software libre", color = Muted, fontSize = 10.sp, maxLines = 1)
            }
            Text("›", color = Accent, fontSize = 18.sp)
        }
    }
}

/** Dialogo con la informacion del desarrollador. */
@Composable
private fun AboutDeveloperDialog(
    githubAvatar: ImageBitmap?,
    onProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier.size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2430), CircleShape)
                            .border(2.dp, Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (githubAvatar != null) {
                            Image(
                                githubAvatar,
                                contentDescription = "Foto de GitHub de Elal Chico",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("EC", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Elal Chico", color = Text1, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Desarrollador de B-Chess", color = Muted, fontSize = 14.sp)
                    }
                    Text(
                        "×",
                        color = Muted,
                        fontSize = 28.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 8.dp)
                    )
                }

                Text(
                    GitHubProfile.BIO,
                    color = Text1,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )

                Button(
                    onClick = onProfile,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Text("Más detalles en GitHub", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
