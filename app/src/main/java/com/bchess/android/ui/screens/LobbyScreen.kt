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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.bchess.android.data.formatClock
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
private val Gold = Color(0xFFC9B15A)
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
            // Pestañas: historial clásico y torneo (aparte, como pidió el usuario)
            var tournamentTab by remember { mutableStateOf(false) }
            val tournamentCount = history.count { it.gameType == "tournament" }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryTab(
                    label = "Historial",
                    selected = !tournamentTab,
                    modifier = Modifier.weight(1f)
                ) { tournamentTab = false }
                HistoryTab(
                    label = "Torneos${if (tournamentCount > 0) " ($tournamentCount)" else ""}",
                    selected = tournamentTab,
                    modifier = Modifier.weight(1f),
                    accent = true
                ) { tournamentTab = true }
            }
            val visibleHistory = history.filter { if (tournamentTab) it.gameType == "tournament" else true }
            // Estadísticas + mini gráfico del conjunto visible
            StatsHud(visibleHistory, tournamentTab)
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
                    onClick = { exportPgn.launch(if (tournamentTab) "b-chess-torneos.pgn" else "chess-x-partidas.pgn") },
                    modifier = Modifier.weight(1f),
                    enabled = visibleHistory.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line2),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Text1),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Guardar PGN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("${visibleHistory.size}/${GameHistoryStore.MAX_RECORDS} partidas mostradas. Se reemplazan automáticamente las más antiguas.", color = Muted, fontSize = 11.sp)
            pgnMessage?.let { Text(it, color = if (it.startsWith("Error:")) Color(0xFFFF9B9B) else Green, fontSize = 11.sp) }
            if (visibleHistory.isEmpty()) {
                Text(
                    if (tournamentTab) "Sin partidas de torneo guardadas. En un torneo por Bluetooth o Internet aparecerán aquí."
                    else "Sin partidas guardadas. Al terminar una por Bluetooth, Internet o contra Stockfish aparecerá aquí.",
                    color = Muted,
                    fontSize = 12.sp
                )
            } else {
                visibleHistory.forEach { rec ->
                    HistoryItem(rec = rec, onReplay = { onReplay(rec) }, onDelete = { onDeleteGame(rec) })
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun HistoryTab(label: String, selected: Boolean, modifier: Modifier = Modifier, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) (if (accent) Color(0x26E2B94E) else Color(0xFF1F2820)) else Card2,
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, if (selected) (if (accent) Gold else Line2) else Line2, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) (if (accent) Gold else Text1) else Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Card, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun StatsHud(records: List<GameRecord>, tournamentTab: Boolean) {
    if (records.isEmpty()) return
    val total = records.size
    val wins = records.count { it.localWon == true }
    val draws = records.count { it.localWon == null }
    val losses = total - wins - draws
    val winRate = if (total > 0) (wins * 100) / total else 0
    val streak = records.takeWhile { it.localWon == true }.count()
    val accent = if (tournamentTab) Gold else Green
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Partidas", total.toString(), Text1, Modifier.weight(1f))
            StatPill("Ganadas", wins.toString(), Green, Modifier.weight(1f))
            StatPill("Empates", draws.toString(), Gold, Modifier.weight(1f))
            StatPill("Perdidas", losses.toString(), Color(0xFFFF9B9B), Modifier.weight(1f))
        }
        // Balance W/D/L en una sola barra proporcional (gráfico visual)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(8.dp)).background(Card)) {
            if (wins > 0) Box(Modifier.weight(wins.toFloat()).fillMaxHeight().background(Green))
            if (draws > 0) Box(Modifier.weight(draws.toFloat()).fillMaxHeight().background(Gold))
            if (losses > 0) Box(Modifier.weight(losses.toFloat()).fillMaxHeight().background(Color(0xFFFF6B6B)))
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Win rate $winRate% · Racha $streak", color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("Última: ${records.firstOrNull()?.dateLabel().orEmpty()}", color = Muted, fontSize = 10.sp)
        }
        // ==== GRÁFICO LINEAL DE DOS EJES - RESULTADOS DE ÚLTIMAS PARTIDAS ====
        Text("Historial de resultados", color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        LineChart(
            points = records.takeLast(20).reversed().mapIndexed { i, r ->
                ChartPoint(
                    x = i.toFloat(),
                    y = when (r.localWon) {
                        true -> 2f   // Victoria
                        null -> 1f   // Empate
                        false -> 0f  // Derrota
                    },
                    color = when (r.localWon) {
                        true -> Green
                        null -> Gold
                        false -> Color(0xFFFF6B6B)
                    },
                    label = r.dateLabel().take(5)
                )
            },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            Box(modifier = Modifier.size(10.dp).background(Green, RoundedCornerShape(2.dp)))
            Text("Victoria", color = Green, fontSize = 10.sp)
            Box(modifier = Modifier.size(10.dp).background(Gold, RoundedCornerShape(2.dp)))
            Text("Empate", color = Gold, fontSize = 10.sp)
            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF6B6B), RoundedCornerShape(2.dp)))
            Text("Derrota", color = Color(0xFFFF6B6B), fontSize = 10.sp)
        }
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

/** Punto de datos para el gráfico lineal de resultados. */
data class ChartPoint(val x: Float, val y: Float, val color: Color, val label: String = "")

/** Gráfico lineal de dos ejes para mostrar historial de resultados de partidas. */
@Composable
private fun LineChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    Canvas(modifier = modifier) {
        val size = size
        val width = size.width
        val height = size.height
        val padding = 16f
        val plotWidth = width - padding * 2
        val plotHeight = height - padding * 2

        if (points.size < 2) return@Canvas

        val minX = points.firstOrNull()?.x ?: 0f
        val maxX = points.lastOrNull()?.x ?: 0f
        val rangeX = if (maxX > minX) maxX - minX else 1f

        // Ejes
        drawLine(
            color = Line,
            start = Offset(padding, padding),
            end = Offset(padding, padding + plotHeight),
            strokeWidth = 1f
        )
        drawLine(
            color = Line,
            start = Offset(padding, padding + plotHeight),
            end = Offset(padding + plotWidth, padding + plotHeight),
            strokeWidth = 1f
        )

        // Dibujar puntos y línea
        for (i in points.indices) {
            val p = points[i]
            val px = padding + ((p.x - minX) / rangeX) * plotWidth
            val py = padding + plotHeight - (p.y / 2f) * plotHeight

            // Línea entre puntos
            if (i > 0) {
                val prev = points[i - 1]
                val prevX = padding + ((prev.x - minX) / rangeX) * plotWidth
                val prevY = padding + plotHeight - (prev.y / 2f) * plotHeight
                drawLine(
                    color = p.color.copy(alpha = 0.7f),
                    start = Offset(prevX, prevY),
                    end = Offset(px, py),
                    strokeWidth = 2f
                )
            }

            // Punto
            drawCircle(
                color = p.color,
                center = Offset(px, py),
                radius = 4f
            )
            drawCircle(
                color = Color.White,
                center = Offset(px, py),
                radius = 2f
            )

            // Etiqueta del eje X (fecha)
            if (i % 5 == 0 || i == points.lastIndex) {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = Muted.toArgb()
                        textSize = 8f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    native.drawText(p.label, px - 15f, padding + plotHeight + 4f, paint)
                }
            }
        }

        // Etiquetas del eje Y
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val paintG = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = Green.toArgb(); textSize = 10f }
            val paintE = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = Gold.toArgb(); textSize = 10f }
            val paintP = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = Color(0xFFFF6B6B).toArgb(); textSize = 10f }
            native.drawText("G", 4f, padding - 6f, paintG)
            native.drawText("E", 4f, padding + plotHeight / 2 - 6f, paintE)
            native.drawText("P", 4f, padding + plotHeight - 4f, paintP)
        }
    }
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
    
    // Resultado del juego
    val resultColor = when {
        rec.localWon == true -> Green
        rec.localWon == false -> Color(0xFFFF6B6B)
        else -> Gold
    }
    val resultText = when {
        rec.localWon == true -> "VICTORIA"
        rec.localWon == false -> "DERROTA"
        else -> "TABLAS"
    }

    // Tiempo promedio por jugada
    val avgTime = if (rec.moveClocks.isNotEmpty()) {
        val totalTime = rec.moveClocks.sum().toDouble()
        val msPerMove = totalTime / rec.moveClocks.size
        formatClock(msPerMove.toLong())
    } else {
        "--"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${rec.dateLabel()} · ${rec.timeLabel}",
                    fontSize = 10.sp, color = Muted, maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(resultColor, CircleShape)
                )
                Text(
                    resultText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultColor,
                    maxLines = 1
                )
            }
            Text(
                "${rec.movesSan.size} jugadas · tiempo prom. ${avgTime} · ${if (rec.localColorWhite) "Blancas" else "Negras"}" +
                (if (rec.gameType == "practice") " · Stockfish" else ""),
                fontSize = 10.sp, color = Muted, maxLines = 2
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onReplay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F), contentColor = Gold),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3524)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A2626)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF3A2222), contentColor = Color(0xFFFF9B9B))
            ) { Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
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
