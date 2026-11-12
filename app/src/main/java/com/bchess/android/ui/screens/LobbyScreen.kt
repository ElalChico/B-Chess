package com.bchess.android.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.bchess.android.data.GameRecord
import com.bchess.android.data.GameHistoryStore
import com.bchess.android.data.LobbyState
import com.bchess.android.data.PgnArchive
import com.bchess.android.ui.components.PhotoCropDialog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

private val Bg = Color(0xFF0B0D10)
private val Card = Color(0xFF171D25)
private val Card2 = Color(0xFF1C232D)
private val Line = Color(0xFF252E39)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Muted = Color(0xFF8B96A5)
private val Gold = Color(0xFFE2B94E)
private val Green = Color(0xFF3DDC97)

private fun decodeOrientedImage(resolver: ContentResolver, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (maxDimension / sampleSize > 2048) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val transform = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(270f)
                postScale(-1f, 1f)
            }
        }
    }
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
}

private fun encodeProfilePhoto(cropped: Bitmap): String {
    val side = minOf(cropped.width, cropped.height)
    var scaled = Bitmap.createScaledBitmap(cropped, minOf(side, 384), minOf(side, 384), true)
    for (quality in listOf(82, 72, 62, 52)) {
        val output = ByteArrayOutputStream()
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) continue
        if (output.size() <= 42_000) return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
    scaled = Bitmap.createScaledBitmap(scaled, 256, 256, true)
    val output = ByteArrayOutputStream()
    if (!scaled.compress(Bitmap.CompressFormat.JPEG, 60, output) || output.size() > 42_000) return ""
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun LobbyScreen(
    lobby: LobbyState,
    localName: String,
    remoteName: String,
    photoBase64: String = "",
    history: List<GameRecord> = emptyList(),
    remoteProfile: com.bchess.android.data.RemoteProfile? = null,
    connected: Boolean = false,
    profileSetupRequired: Boolean = false,
    onViewRivalProfile: () -> Unit = {},
    continueLabel: String = "Continuar",
    onContinue: () -> Unit = {},
    onNameChange: (String) -> Unit,
    onReplay: (GameRecord) -> Unit = {},
    onDeleteGame: (GameRecord) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onImportGames: (List<GameRecord>) -> Int = { 0 },
    onPhotoCropped: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var pendingCrop by remember { mutableStateOf<Bitmap?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var pgnMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoError = null
        if (uri != null) {
            try {
                pendingCrop = decodeOrientedImage(context.contentResolver, uri)
                    ?.let { downscale(it, 2048) }
                if (pendingCrop == null && photoError == null) photoError = "No se pudo abrir la imagen"
            } catch (e: Exception) {
                pendingCrop = null
                photoError = "No se pudo abrir la imagen"
            }
        }
    }
    val exportPgn = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-chess-pgn")) { uri ->
        pgnMessage = null
        if (uri != null) {
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("No se pudo crear el archivo PGN.")
                output.bufferedWriter(StandardCharsets.UTF_8).use {
                    it.write(GameHistoryStore.exportPgn(history))
                }
                pgnMessage = "Partidas guardadas en el documento seleccionado."
            } catch (e: IOException) {
                pgnMessage = "Error: ${e.message ?: "No se pudo guardar el archivo PGN."}"
            } catch (e: SecurityException) {
                pgnMessage = "Error: no se concedió permiso para guardar el archivo PGN."
            }
        }
    }
    val importPgn = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pgnMessage = null
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("No se pudo abrir el archivo PGN.")
                val text = input.use(::readPgnArchive)
                val imported = PgnArchive.parse(text)
                val added = onImportGames(imported)
                pgnMessage = if (added == 0) {
                    "No hay partidas nuevas para importar."
                } else {
                    "Partidas importadas: $added. El historial conserva un máximo de ${GameHistoryStore.MAX_RECORDS}."
                }
            } catch (e: IOException) {
                pgnMessage = "Error: ${e.message ?: "No se pudo leer el archivo PGN."}"
            } catch (e: SecurityException) {
                pgnMessage = "Error: no se concedió permiso para leer el archivo PGN."
            } catch (e: IllegalArgumentException) {
                pgnMessage = "Error: ${e.message ?: "El archivo no contiene un PGN compatible."}"
            } catch (e: java.text.ParseException) {
                pgnMessage = "Error: la fecha de una partida PGN no es válida."
            }
        }
    }

    pendingCrop?.let { bmp ->
        PhotoCropDialog(
            bitmap = bmp,
            onConfirm = { cropped ->
                try {
                    val b64 = encodeProfilePhoto(cropped)
                    if (b64.isBlank()) {
                        photoError = "No se pudo preparar la foto. Prueba con otra imagen."
                    } else {
                        onPhotoCropped(b64)
                        photoError = null
                        pendingCrop = null
                    }
                } catch (e: Exception) {
                    photoError = "No se pudo guardar la foto. Prueba con otra imagen."
                }
            },
            onDismiss = { pendingCrop = null }
        )
    }

    // Inicializar nombre: vacío si primera vez, o nombre guardado para editar
    LaunchedEffect(localName, profileSetupRequired) {
        nameText = if (profileSetupRequired && localName == "Jugador") "" else localName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        photoError?.let {
            Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
        }
        if (!profileSetupRequired) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                Text("← Volver al inicio", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (profileSetupRequired) "Crea tu perfil" else "Perfil", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Text1)
            if (!profileSetupRequired && remoteName.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF12241B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3B2B))
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(7.dp).background(Green, RoundedCornerShape(50)))
                        Text("Conectado", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9BE3BD))
                    }
                }
            }
        }
        Text(
            if (profileSetupRequired) "Paso 1 de 3 · El rival reconocerá tu nombre y foto."
            else if (remoteName.isBlank()) "Foto, nombre e historial. Conecta para jugar."
            else "Rival: $remoteName · abre la sala para jugar.",
            color = Muted,
            fontSize = 12.sp
        )

        if (!profileSetupRequired && connected && remoteProfile != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF3A3524), RoundedCornerShape(14.dp))
                    .clickable { onViewRivalProfile() }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RemoteAvatarMini(remoteProfile.photoBase64, remoteProfile.name.ifBlank { "R" })
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Ver perfil de ${remoteProfile.name.ifBlank { remoteName.ifBlank { "Rival" } }}",
                            color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1
                        )
                        Text(
                            "${remoteProfile.total} jugadas · ${remoteProfile.wins}V · ${remoteProfile.losses}D · ${remoteProfile.draws}E",
                            color = Muted, fontSize = 11.sp
                        )
                    }
                    Text("→", color = Gold, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Profile photo (round avatar + crop)
        SectionLabel("FOTO DE PERFIL")
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(photoBase64, localName.ifBlank { "T" })
            Spacer(Modifier.width(14.dp))
            Column {
                Button(
                    onClick = { picker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27342F), contentColor = Color(0xFFB8D0C2)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Text(
                        when {
                            profileSetupRequired && photoBase64.isBlank() -> "Agregar foto"
                            profileSetupRequired -> "Cambiar foto"
                            else -> "Cambiar foto"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (profileSetupRequired) "Elige una foto para completar tu perfil."
                    else "Pellizca para acercar · arrastra para posicionar",
                    color = Muted,
                    fontSize = 11.sp
                )
                photoError?.let { Text(it, color = Color(0xFFFF9B9B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        // Name field
        SectionLabel("NOMBRE DEL PERFIL")
        var hasChanges by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = nameText,
            onValueChange = {
                if (it.length <= 18) {
                    nameText = it
                    if (it.isNotBlank()) onNameChange(it)
                    hasChanges = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            label = { Text("Tu nombre", color = Muted) },
            placeholder = { Text("Escribe el nombre que usarás", color = Muted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Text1, unfocusedTextColor = Text1,
                cursorColor = Gold, focusedBorderColor = Gold, unfocusedBorderColor = Line2,
                focusedContainerColor = Color(0xFF0F141B), unfocusedContainerColor = Color(0xFF0F141B)
            )
        )
        Text(
            if (profileSetupRequired) "Este es el nombre que verán tus rivales."
            else "El nombre actual está cargado; puedes editarlo o reemplazarlo.",
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        if (hasChanges && nameText.isNotBlank()) {
            Text("Nombre guardado automáticamente", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }

        val canContinue = nameText.isNotBlank() && (!profileSetupRequired || photoBase64.isNotBlank())
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            enabled = canContinue,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF789889),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF29312E),
                disabledContentColor = Muted
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(continueLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (profileSetupRequired && !canContinue) {
            Text(
                if (nameText.isBlank()) "Escribe tu nombre para continuar."
                else "Agrega una foto para completar el perfil.",
                color = Muted,
                fontSize = 11.sp
            )
        }

        // History (PGN) — each entry replays with its captured data
        if (!profileSetupRequired) Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("HISTORIAL (PGN)")
            Spacer(Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Borrar todo", color = Color(0xFFFF9B9B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (!profileSetupRequired) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { importPgn.launch(arrayOf("application/x-chess-pgn", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line2),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Cargar PGN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { exportPgn.launch("chess-x-partidas.pgn") },
                    modifier = Modifier.weight(1f),
                    enabled = history.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line2),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Guardar PGN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("${history.size}/${GameHistoryStore.MAX_RECORDS} partidas guardadas. Se reemplazan automáticamente las más antiguas.", color = Muted, fontSize = 11.sp)
            pgnMessage?.let { Text(it, color = if (it.startsWith("Error:")) Color(0xFFFF9B9B) else Green, fontSize = 11.sp) }
            if (history.isEmpty()) {
                Text("Sin partidas guardadas. Al terminar una por Bluetooth aparecerá aquí.", color = Muted, fontSize = 12.sp)
            } else {
                history.forEach { rec ->
                    HistoryItem(rec = rec, onReplay = { onReplay(rec) }, onDelete = { onDeleteGame(rec) })
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

private fun readPgnArchive(input: InputStream): String {
    val text = StringBuilder()
    input.bufferedReader(StandardCharsets.UTF_8).use { reader ->
        val buffer = CharArray(8192)
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break
            if (text.length + read > PgnArchive.MAX_ARCHIVE_CHARS) {
                throw IOException("El archivo PGN supera el tamaño máximo permitido.")
            }
            text.append(buffer, 0, read)
        }
    }
    return text.toString()
}

private fun downscale(src: Bitmap?, maxDim: Int): Bitmap? {
    if (src == null) return null
    val longest = maxOf(src.width, src.height)
    if (longest <= maxDim) return src
    val ratio = maxDim.toFloat() / longest.toFloat()
    val out = Bitmap.createScaledBitmap(src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true)
    if (out !== src) src.recycle()
    return out
}

fun decodePhoto(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    val raw = base64.substringAfter("base64,", base64)
    return runCatching {
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@Composable
private fun HistoryItem(rec: GameRecord, onReplay: () -> Unit, onDelete: () -> Unit) {
    // Determine white and black player names based on who played white
    val (whiteName, blackName) = if (rec.localColorWhite) {
        rec.playerName to rec.opponent
    } else {
        rec.opponent to rec.playerName
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("$whiteName vs $blackName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Text1, maxLines = 1)
            Text(
                "${rec.dateLabel()} · ${rec.result} · ${rec.movesSan.size} jugadas · ${rec.timeLabel}" +
                    if (rec.localColorWhite) " · Juegas blancas" else " · Juegas negras",
                fontSize = 11.sp, color = Muted, maxLines = 2
            )
        }
        Button(
            onClick = onReplay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Gold),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3524)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) { Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(
            onClick = onDelete,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A2626)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF3A2222), contentColor = Color(0xFFFF9B9B))
        ) { Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}

@Composable
internal fun RemoteAvatarMini(photo: String?, name: String) {
    val bmp = remember(photo) { decodePhoto(photo) }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A2433))
            .border(1.dp, Color(0xFF24324A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = name, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Text((name.firstOrNull() ?: 'R').uppercaseChar().toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Text1)
        }
    }
}

@Composable
fun ProfileAvatar(photoBase64: String, name: String) {
    val bmp = remember(photoBase64) { decodePhoto(photoBase64) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFF15251D))
            .border(2.dp, Green.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                (name.firstOrNull() ?: 'T').uppercaseChar().toString(),
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Green
            )
        }
    }
}
