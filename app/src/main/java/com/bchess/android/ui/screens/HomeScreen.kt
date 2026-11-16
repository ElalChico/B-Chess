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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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

// ===== Sistema visual del Home =====
private val Bg = Color(0xFF0D1113)
private val BgTop = Color(0xFF151C1F)
private val Card = Color(0xFF161C1F)
private val CardDeep = Color(0xFF111719)
private val Line = Color(0xFF263134)
private val LineSoft = Color(0xFF1E272A)
private val Text1 = Color(0xFFF1F4F2)
private val Muted = Color(0xFF95A29D)
private val Muted2 = Color(0xFF6C7A76)
private val Accent = Color(0xFF7FA795)
private val AccentSoft = Color(0xFF23302B)
private val Green = Color(0xFF5FD3A0)
private val Gold = Color(0xFFC9B15A)
private val GoldSoft = Color(0xFF272217)
private val Danger = Color(0xFFFF7A7A)
private val DangerSoft = Color(0xFF201416)

private val ShapeCard = RoundedCornerShape(18.dp)
private val ShapeChip = RoundedCornerShape(50)

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
    onTournament: () -> Unit = {},
    onFinishConnection: () -> Unit = {},
    onOpenConexiones: () -> Unit = {},
    remoteName: String = "",
    onExit: () -> Unit = {}
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgTop, Bg))
            )
    ) {
        // Resplandor tenue en la parte superior: da profundidad sin distraer
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Accent.copy(alpha = 0.09f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ================= CABECERA =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Accent.copy(alpha = 0.22f), Accent.copy(alpha = 0.06f))
                                )
                            )
                            .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(com.bchess.android.R.drawable.chessx_mark),
                            contentDescription = "Logo de B-Chess",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "B-Chess",
                            color = Text1,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            "Ajedrez local · Bluetooth · Torneos",
                            color = Muted,
                            fontSize = 11.5.sp
                        )
                    }
                }
                StatusChip(connected = inSession)
            }

            // ================= PERFIL =================
            ProfileHero(
                avatar = avatar,
                profileName = profileName,
                profileConfigured = profileConfigured,
                onClick = onProfile
            )

            // ================= TÍTULO DE SECCIÓN =================
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "SIGUIENTE PASO",
                    color = Muted2,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    if (profileConfigured) "Elige cómo jugar" else "Completa tu perfil para empezar",
                    color = Text1,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ================= ACCIONES PRINCIPALES =================
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeAction(
                    title = "Crear partida",
                    subtitle = "Ser anfitrión",
                    icon = Icons.Outlined.AddCircle,
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = onCreate
                )
                HomeAction(
                    title = "Unirse",
                    subtitle = "Buscar anfitrión",
                    icon = Icons.Outlined.Groups,
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onSearch
                )
            }

            // ================= MODOS DE JUEGO =================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "MODOS DE JUEGO",
                    color = Muted2,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCard,
                    color = Card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Column {
                        ModeRow(
                            icon = { Icon(Icons.Outlined.EmojiEvents, null, tint = Gold, modifier = Modifier.size(21.dp)) },
                            iconBackground = GoldSoft,
                            title = "Torneo",
                            subtitle = "Partidas múltiples · gana la serie",
                            onClick = onTournament
                        )
                        HorizontalDivider(color = LineSoft)
                        ModeRow(
                            icon = {
                                Icon(
                                    painter = painterResource(com.bchess.android.R.drawable.ic_stockfish),
                                    contentDescription = "Stockfish",
                                    tint = Accent,
                                    modifier = Modifier.size(21.dp)
                                )
                            },
                            iconBackground = AccentSoft,
                            title = "Practicar contra Stockfish",
                            subtitle = "Motor local con fuerza ajustable",
                            onClick = onPractice
                        )
                        HorizontalDivider(color = LineSoft)
                        ModeRow(
                            icon = { Icon(Icons.Outlined.Link, null, tint = Green, modifier = Modifier.size(21.dp)) },
                            iconBackground = Color(0xFF16261F),
                            title = "Conexiones",
                            subtitle = "Crear sala, unirse o ver el perfil del rival · BT · P2P · red local",
                            onClick = onOpenConexiones
                        )
                    }
                }
            }

            // ================= SESIÓN ACTIVA =================
            if (inSession) {
                SessionCard(
                    remoteName = remoteName,
                    isGuestSession = isGuestSession,
                    onOpen = onOpenConexiones,
                    onFinish = onFinishConnection
                )
            }

            // ================= RELOJ POR DEFECTO =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = CardDeep,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineSoft)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = Accent, modifier = Modifier.size(19.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Reloj por defecto", color = Text1, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (inSession) "Se usa en las nuevas partidas y torneos"
                            else "Individual o torneo · el rival se conecta cerca",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        Modifier
                            .clip(ShapeChip)
                            .background(Accent.copy(alpha = 0.12f))
                            .border(1.dp, Accent.copy(alpha = 0.30f), ShapeChip)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(clockSummary, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ================= FIRMA DEL DESARROLLADOR =================
            DeveloperCard(
                githubAvatar = githubAvatar,
                onAbout = { showAboutDeveloper = true },
                onProfile = { openUrl(GitHubProfile.PROFILE_URL) }
            )

            // ================= SALIR =================
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger, containerColor = DangerSoft.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("SALIR", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(4.dp))
        }
    }

    if (showAboutDeveloper) {
        AboutDeveloperDialog(
            githubAvatar = githubAvatar,
            onProfile = { openUrl(GitHubProfile.PROFILE_URL) },
            onDismiss = { showAboutDeveloper = false }
        )
    }
}

/** Chip de estado de sesión en la cabecera. */
@Composable
private fun StatusChip(connected: Boolean) {
    Surface(
        shape = ShapeChip,
        color = if (connected) Color(0xFF16261F) else Card,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (connected) Green.copy(alpha = 0.40f) else Line
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(if (connected) Green else Muted2, CircleShape)
            )
            Text(
                if (connected) "Conectado" else "Sin conexión",
                color = if (connected) Green else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Tarjeta principal del perfil: foto, nombre y acceso a la edición. */
@Composable
private fun ProfileHero(
    avatar: ImageBitmap?,
    profileName: String,
    profileConfigured: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF131A1D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.28f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Accent.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF1A2429), CircleShape)
                    .border(1.5.dp, if (profileConfigured) Accent else Gold.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Solo la foto que configuro el usuario. Nunca se muestra el
                // avatar de GitHub del desarrollador aqui (solo en su tarjeta).
                if (avatar != null) {
                    Image(
                        avatar,
                        contentDescription = "Foto de $profileName",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        profileName.firstOrNull()?.uppercaseChar()?.toString() ?: "J",
                        color = if (profileConfigured) Accent else Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    profileName.ifBlank { "Completa tu perfil" },
                    color = Text1,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    if (profileConfigured) "Perfil, foto y descripción" else "Agrega tu nombre y foto para empezar",
                    color = if (profileConfigured) Muted else Gold,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Abrir perfil",
                tint = Accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Botón de acción principal (crear / unirse). */
@Composable
private fun HomeAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (primary) Color(0xFF1B2622) else Card,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (primary) Accent.copy(alpha = 0.45f) else Line
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    if (primary) Brush.verticalGradient(
                        listOf(Accent.copy(alpha = 0.16f), Color.Transparent)
                    ) else Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                )
                .padding(horizontal = 15.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (primary) Accent.copy(alpha = 0.20f) else CardDeep),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (primary) Accent else Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Text1
                )
                Text(
                    subtitle,
                    color = if (primary) Accent else Muted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Fila dentro de la lista de modos de juego. */
@Composable
private fun ModeRow(
    icon: @Composable () -> Unit,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(iconBackground),
            contentAlignment = Alignment.Center
        ) { icon() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Text1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = Muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Muted2,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Panel de sesión activa con acceso a conexiones y cierre de sesión. */
@Composable
private fun SessionCard(
    remoteName: String,
    isGuestSession: Boolean,
    onOpen: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeCard,
        color = Color(0xFF13201B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Green.copy(alpha = 0.38f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(Green, CircleShape)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Sesión activa · ${remoteName.ifBlank { "Rival" }}",
                        color = Text1,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        if (isGuestSession) "Conectado · esperando al anfitrión"
                        else "Conectado · listos para comenzar",
                        color = Green,
                        fontSize = 11.5.sp
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF0C1411)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    Text("Abrir conexiones", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onFinish,
                    modifier = Modifier.heightIn(min = 46.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger, containerColor = DangerSoft.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text("Finalizar", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
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
        shape = ShapeCard,
        color = CardDeep,
        border = androidx.compose.foundation.BorderStroke(1.dp, LineSoft),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAbout)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2429), CircleShape)
                    .border(1.dp, Accent.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (githubAvatar != null) {
                    Image(
                        githubAvatar,
                        contentDescription = "Foto de GitHub de Elal Chico",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("EC", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Creado por Elal Chico", color = Text1, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Desarrollador autodidacta · Software libre", color = Muted, fontSize = 10.5.sp, maxLines = 1)
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Ver información",
                tint = Muted2,
                modifier = Modifier.size(17.dp)
            )
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
                            .background(Color(0xFF1A2429), CircleShape)
                            .border(2.dp, Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (githubAvatar != null) {
                            Image(
                                githubAvatar,
                                contentDescription = "Foto de GitHub de Elal Chico",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF0C1411)),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Text("Más detalles en GitHub", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
