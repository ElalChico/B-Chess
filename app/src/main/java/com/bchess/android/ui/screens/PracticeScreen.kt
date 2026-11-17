package com.bchess.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.bchess.android.core.ChessEngine
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.core.GameEnd
import com.bchess.android.core.GameEndReason
import com.bchess.android.core.Move
import com.bchess.android.core.PieceType
import com.bchess.android.Screen
import com.bchess.android.data.AppPreferences
import com.bchess.android.data.ClockManager
import com.bchess.android.data.ClockState
import com.bchess.android.data.EngineInstallState
import com.bchess.android.data.GameHistoryStore
import com.bchess.android.data.GameRecord
import com.bchess.android.data.GAME_TYPE_PRACTICE
import com.bchess.android.data.PieceSet
import com.bchess.android.data.PredefinedControls
import com.bchess.android.data.LocalChessEngine
import com.bchess.android.data.SoundManager
import com.bchess.android.data.SoundType
import com.bchess.android.data.strengthLevelName
import com.bchess.android.data.BoardTheme
import com.bchess.android.data.TimeControlOption
import com.bchess.android.data.formatClock
import com.bchess.android.data.buildOgn
import com.bchess.android.ui.components.BoardSelection
import com.bchess.android.ui.components.ChessBoard
import com.bchess.android.ui.components.GameEndDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PracticeBg = Color(0xFF0B0D10)
private val PracticeCard = Color(0xFF151A20)
private val PracticeLine = Color(0xFF2A3038)
private val PracticeText = Color(0xFFEEF1F5)
private val PracticeMuted = Color(0xFF8B96A5)
private val PracticeGold = Color(0xFFC9B15A)
private val PracticeGreen = Color(0xFF3DDC97)
private val PracticeRed = Color(0xFFFF7777)

/** Victorias necesarias en el nivel actual para subir de nivel automáticamente. */
private const val PROGRESSIVE_WINS_NEEDED = 6

/** Cantidad de nivel (en %) que sube la fuerza al completar la progresión. */
private const val PROGRESSIVE_STEP = 10

@Composable
fun PracticeScreen(
    engineManager: LocalChessEngine,
    boardTheme: BoardTheme,
    pieceSet: PieceSet,
    showCoordinates: Boolean,
    moveIndicators: Boolean,
    checkSignal: Boolean,
    initialTimeMs: Long = 300_000L,
    initialIncrementMs: Long = 0L,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStockfishSettings: () -> Unit,
    prefs: AppPreferences,
    onReplay: (GameRecord) -> Unit,
    sound: SoundManager? = null,
    onGameStateChange: ((Boolean) -> Unit)? = null
) {
    val engine = remember { ChessEngine() }
    val coroutineScope = rememberCoroutineScope()
    val installState by engineManager.installState.collectAsState()
    val moveHistory = remember { mutableStateListOf<String>() }
    var snapshot by remember { mutableStateOf(engine.snapshot) }
    var selection by remember { mutableStateOf(BoardSelection(null)) }
    // Fuerza del motor: se guarda en preferencias para mantenerse entre sesiones
    var strengthPercent by remember { mutableStateOf(prefs.strengthPercent) }
    var progressiveEnabled by remember { mutableStateOf(prefs.progressiveLevel) }
    var progressiveWins by remember { mutableStateOf(prefs.progressiveWins) }
    // Contador de finales de partida: dispara la lógica progresiva una sola vez por partida
    var gameEndCount by remember { mutableStateOf(0) }
    // null = aleatorio
    var playerColor by remember { mutableStateOf<ChessColor?>(ChessColor.WHITE) }
    var gameStarted by remember { mutableStateOf(false) }
    var timeControl by remember(initialTimeMs, initialIncrementMs) {
        mutableStateOf(
            PredefinedControls.options.firstOrNull {
                it.timeMs == initialTimeMs && it.incrementMs == initialIncrementMs
            } ?: TimeControlOption(
                label = if (initialTimeMs == 0L) "Ilimitado" else
                    "${initialTimeMs / 60_000} + ${initialIncrementMs / 1_000}",
                timeMs = initialTimeMs,
                incrementMs = initialIncrementMs
            )
        )
    }
    var thinking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var gameEnd by remember { mutableStateOf<GameEnd?>(null) }
    LaunchedEffect(gameStarted, gameEnd) { onGameStateChange?.invoke(gameStarted && gameEnd == null) }

    var showSidePicker by remember { mutableStateOf(false) }
    var lastMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endDialogDismissed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Subida progresiva: al ganar 6 partidas con el nivel actual, el nivel sube solo
    LaunchedEffect(gameEndCount) {
        if (gameEndCount == 0) return@LaunchedEffect
        val end = gameEnd ?: return@LaunchedEffect
        val currentColor = playerColor ?: return@LaunchedEffect
        if (end.winner != currentColor) return@LaunchedEffect
        if (!prefs.progressiveLevel) return@LaunchedEffect
        val wins = prefs.progressiveWins + 1
        if (wins >= PROGRESSIVE_WINS_NEEDED) {
            prefs.setProgressiveWins(0)
            progressiveWins = 0
            val next = (strengthPercent + PROGRESSIVE_STEP).coerceAtMost(100)
            strengthPercent = next
            prefs.setStrengthPercent(next)
            android.widget.Toast.makeText(
                context,
                "¡Nivel subido a $next% · ${strengthLevelName(next)}!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            prefs.setProgressiveWins(wins)
            progressiveWins = wins
        }
    }
    val playerName = prefs.profileName
    val clockManager = remember(coroutineScope) {
        ClockManager(
            scope = coroutineScope,
            isHost = true,
            onSyncRequest = {},
            onTimeout = { loser ->
                val winner = loser.opposite()
                gameEnd = GameEnd(
                    winner = winner,
                    reason = GameEndReason.TIMEOUT,
                    message = if (loser == playerColor) "Te quedaste sin tiempo" else "Stockfish se quedó sin tiempo"
                )
                gameEndCount++
                sound?.play(if (loser == playerColor) SoundType.LOSE else SoundType.WIN)
            }
        )
    }
    val clock by clockManager.state.collectAsState()
    DisposableEffect(engineManager, clockManager) {
        onDispose {
            clockManager.stop()
            // engineManager es un singleton compartido: NO se detiene aquí
            // para evitar corromper el estado de Stockfish al cambiar de pantalla
        }
    }

    fun refreshAfterMove(move: Move) {
        snapshot = engine.snapshot
        lastMove = move.from to move.to
        val checkedKing = if (checkSignal && engine.isInCheck(engine.sideToMove())) {
            engine.findKing(engine.sideToMove())
        } else {
            null
        }
        selection = BoardSelection(null, lastMove = lastMove, checkKingSquare = checkedKing)
        gameEnd = engine.detectEnd()
        if (gameEnd != null) {
            clockManager.finish()
            gameEndCount++
        }
    }

    // Sonido de jugada y de fin de partida: captura/jaque/mate (sin sonido de colocación)
    fun playMoveSound(captured: Boolean) {
        when {
            gameEnd?.reason == GameEndReason.CHECKMATE -> sound?.play(SoundType.CHECKMATE)
            gameEnd != null -> sound?.play(SoundType.DRAW)
            engine.isInCheck(engine.sideToMove()) -> sound?.play(SoundType.CHECK)
            captured -> sound?.play(SoundType.CAPTURE)
        }
        val winner = gameEnd?.winner
        if (winner != null) {
            sound?.play(if (winner == playerColor) SoundType.WIN else SoundType.LOSE)
        }
    }

    fun requestEngineMove() {
        if (gameEnd != null || !gameStarted) return
        thinking = true
        error = null
        coroutineScope.launch {
            try {
                val response = engineManager.bestMove(moveHistory.toList(), strengthPercent)
                if (gameEnd != null) return@launch
                val move = engine.legalMoves().firstOrNull { it.uci() == response }
                    ?: throw IllegalStateException("Stockfish devolvió un movimiento no válido")
                val mover = engine.sideToMove()
                val captured = engine.pieceAt(move.to) != null
                if (!engine.doMove(move)) throw IllegalStateException("No se pudo aplicar el movimiento del motor")
                moveHistory.add(move.uci())
                clockManager.onMovePlayed(mover)
                refreshAfterMove(move)
                playMoveSound(captured)
            } catch (e: Exception) {
                error = e.message ?: "No se pudo obtener el movimiento del motor"
            } finally {
                thinking = false
            }
        }
    }

    fun resetGame() {
        endDialogDismissed = false
        clockManager.stop()
        clockManager.configure(timeControl.timeMs, timeControl.incrementMs)
        engine.reset()
        moveHistory.clear()
        snapshot = engine.snapshot
        selection = BoardSelection(null)
        gameEnd = null
        lastMove = null
        error = null
    }

    fun startNewGame(side: ChessColor?) {
        if (!engineManager.isInstalled()) {
            error = (installState as? EngineInstallState.Error)?.message ?: "Stockfish no está disponible"
            return
        }
        playerColor = side ?: (if (kotlin.random.Random.nextBoolean()) ChessColor.WHITE else ChessColor.BLACK)
        resetGame()
        gameStarted = true
        clockManager.start()
        if (side == ChessColor.BLACK) requestEngineMove()
    }

    fun prepareNewGame(side: ChessColor?) {
        playerColor = side
        gameStarted = false
        resetGame()
    }

    fun onSquareTap(square: Int) {
        val currentColor = playerColor ?: return
        if (!gameStarted || thinking || gameEnd != null || engine.sideToMove() != currentColor) return
        if (!engineManager.isInstalled()) {
            error = (installState as? EngineInstallState.Error)?.message ?: "Stockfish no está disponible"
            return
        }
        val piece = engine.pieceAt(square)
        val selected = selection
        if (piece?.color == playerColor) {
            selection = BoardSelection(
                from = square,
                legalTargets = engine.legalMovesFrom(square).map { it.to }.distinct(),
                lastMove = lastMove
            )
            // Selección de pieza: sin sonido (desactivado a petición del usuario)
            error = null
            return
        }
        val from = selected.from
        if (from != null && square in selected.legalTargets) {
            val move = engine.legalMoves().firstOrNull {
                it.from == from && it.to == square && (it.promotion == null || it.promotion == PieceType.QUEEN)
            } ?: run {
                selection = BoardSelection(null, lastMove = lastMove)
                return
            }
            val captured = engine.pieceAt(move.to) != null
            if (!engine.doMove(move)) {
                error = "No se pudo aplicar el movimiento"
                return
            }
            moveHistory.add(move.uci())
            val currentColor = playerColor ?: ChessColor.WHITE
            clockManager.onMovePlayed(currentColor)
            refreshAfterMove(move)
            playMoveSound(captured)
            if (gameEnd == null) requestEngineMove()
        } else {
            if (selected.from != null) sound?.play(SoundType.ILLEGAL)
            selection = BoardSelection(null, lastMove = lastMove)
        }
    }

    if (!gameStarted) {
        PracticeSetup(
            installState = installState,
            installed = engineManager.isInstalled(),
            strengthPercent = strengthPercent,
            onStrengthChange = { value ->
                if (value != strengthPercent) {
                    prefs.setProgressiveWins(0)
                    progressiveWins = 0
                }
                strengthPercent = value
                prefs.setStrengthPercent(value)
            },
            progressiveEnabled = progressiveEnabled,
            onProgressiveChange = { enabled ->
                progressiveEnabled = enabled
                prefs.setProgressiveLevel(enabled)
                if (!enabled) { prefs.setProgressiveWins(0); progressiveWins = 0 }
            },
            progressiveWins = progressiveWins,
            timeControl = timeControl,
            timeControls = (PredefinedControls.options + timeControl).distinctBy { it.timeMs to it.incrementMs },
            onTimeControlChange = { timeControl = it },
            playerColor = playerColor,
            onPlayerColorChange = { playerColor = it },
            onBack = onBack,
            onStart = { startNewGame(playerColor) },
            error = error
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(PracticeBg).padding(10.dp)
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        Modifier.weight(0.44f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PracticeHeader(
                            onBack = onBack,
                            onRestart = { startNewGame(playerColor) },
                            enabled = !thinking,
                            strengthPercent = strengthPercent,
                            timeLabel = timeControl.label,
                            onOpenSettings = { onOpenStockfishSettings() },
                            profilePhoto = prefs.profilePhotoBase64,
                            playerColor = playerColor
                        )
                        EngineStatusRow(
                            installState = installState,
                            installed = engineManager.isInstalled()
                        )
                        PracticeClockBar(clock, timeControl, engine.sideToMove())
                        Spacer(Modifier.weight(1f))
                        if (gameEnd != null && endDialogDismissed) {
                            PracticeReopenResultButton { endDialogDismissed = false }
                        }
                        PracticeGameStatus(
                            gameEnd = gameEnd,
                            thinking = thinking,
                            sideToMove = engine.sideToMove(),
                            playerColor = playerColor,
                            error = error
                        )
                    }
                    PracticeBoard(
                        modifier = Modifier.weight(0.56f).fillMaxHeight(),
                        snapshot = snapshot,
                        selection = selection,
                        onSquareTap = ::onSquareTap,
                        boardTheme = boardTheme,
                        pieceSet = pieceSet,
                        showCoordinates = showCoordinates,
                        playerColor = playerColor,
                        moveIndicators = moveIndicators
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PracticeHeader(
                        onBack = onBack,
                        onRestart = { startNewGame(playerColor) },
                        enabled = !thinking,
                        strengthPercent = strengthPercent,
                        timeLabel = timeControl.label,
                        onOpenSettings = { onOpenStockfishSettings() },
                        profilePhoto = prefs.profilePhotoBase64,
                        playerColor = playerColor
                    )
                    EngineStatusRow(
                        installState = installState,
                        installed = engineManager.isInstalled()
                    )
                    PracticeClockBar(clock, timeControl, engine.sideToMove())
                    PracticeBoard(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        snapshot = snapshot,
                        selection = selection,
                        onSquareTap = ::onSquareTap,
                        boardTheme = boardTheme,
                        pieceSet = pieceSet,
                        showCoordinates = showCoordinates,
                        playerColor = playerColor,
                        moveIndicators = moveIndicators
                    )
                    if (gameEnd != null && endDialogDismissed) {
                        PracticeReopenResultButton { endDialogDismissed = false }
                    }
                    PracticeGameStatus(
                        gameEnd = gameEnd,
                        thinking = thinking,
                        sideToMove = engine.sideToMove(),
                        playerColor = playerColor,
                        error = error
                    )
                }
            }

            if (gameEnd != null && !endDialogDismissed) {
                val end = gameEnd!!
                val localWon = end.winner?.let { it == playerColor }
                val record = GameRecord(
                    opponent = "Stockfish",
                    playerName = playerName,
                    result = when (end.winner) {
                        null -> "1/2-1/2"
                        playerColor -> "1-0"
                        else -> "0-1"
                    },
                    localWon = localWon,
                    localColorWhite = playerColor == ChessColor.WHITE,
                    timeLabel = timeControl.label,
                    movesSan = moveHistory.toList(),
                    pgn = "",
                    finalFen = engine.toFEN(),
                    uciMoves = moveHistory.toList(),
                    gameType = GAME_TYPE_PRACTICE
                )
                GameEndDialog(
                    end = end,
                    isHost = true,
                    tournamentActive = false,
                    tournamentFinished = false,
                    localName = playerName,
                    remoteName = "Stockfish",
                    localPhotoBase64 = "",
                    remotePhotoBase64 = null,
                    winnerName = when (end.winner) {
                        null -> null
                        playerColor -> playerName
                        else -> "Stockfish"
                    },
                    onNewGameSame = { startNewGame(playerColor) },
                    onNewGameRandom = { startNewGame(if (kotlin.random.Random.nextBoolean()) ChessColor.WHITE else ChessColor.BLACK) },
                    onTournamentNext = { },
                    onBackToRoom = { resetGame() },
                    onReplayPgn = {
                        GameHistoryStore.append(context, record)
                        onReplay(record)
                    },
                    onDismiss = { endDialogDismissed = true }
                )
            }

            if (showSidePicker) {
                PracticeSidePicker(
                    onDismiss = { showSidePicker = false },
                    onChoose = { side ->
                        showSidePicker = false
                        prepareNewGame(side)
                    }
                )
            }
        }
    }
}

@Composable
private fun PracticeSetup(
    installState: EngineInstallState,
    installed: Boolean,
    strengthPercent: Int,
    onStrengthChange: (Int) -> Unit,
    progressiveEnabled: Boolean,
    onProgressiveChange: (Boolean) -> Unit,
    progressiveWins: Int,
    timeControl: TimeControlOption,
    timeControls: List<TimeControlOption>,
    onTimeControlChange: (TimeControlOption) -> Unit,
    playerColor: ChessColor?,
    onPlayerColorChange: (ChessColor?) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    error: String?
) {
    Column(
        modifier = Modifier.fillMaxSize().background(PracticeBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("← Inicio", color = PracticeMuted, fontSize = 12.sp)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("Preparar partida", color = PracticeText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Contra Stockfish", color = PracticeMuted, fontSize = 11.sp)
            }
        }
        EngineStatusRow(installState = installState, installed = installed)
        // Presentación del rival con su imagen
        StockfishPresentation()
        Text("Configura antes de comenzar", color = PracticeText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        PracticeLevel(strengthPercent, onStrengthChange, enabled = installed)
        PracticeProgressive(
            enabled = progressiveEnabled,
            wins = progressiveWins,
            onToggle = onProgressiveChange
        )
        Column(
            modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
                .border(1.dp, PracticeLine, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tiempo por jugador", color = PracticeText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            TimeControlPicker(timeControl, timeControls, onTimeControlChange)
            Text(
                if (timeControl.timeMs == 0L) "Sin límite de tiempo"
                else "${timeControl.timeMs / 60_000} min · incremento ${timeControl.incrementMs / 1_000} s",
                color = PracticeMuted,
                fontSize = 11.sp
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
                .border(1.dp, PracticeLine, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Elige tu color", color = PracticeText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PracticeColorOption("Blancas", playerColor == ChessColor.WHITE, Modifier.weight(1f)) {
                    onPlayerColorChange(ChessColor.WHITE)
                }
                PracticeColorOption("Negras", playerColor == ChessColor.BLACK, Modifier.weight(1f)) {
                    onPlayerColorChange(ChessColor.BLACK)
                }
                PracticeColorOption("Aleatorio", playerColor == null, Modifier.weight(1f)) {
                    onPlayerColorChange(null)
                }
            }
        }
        if (error != null) {
            Text(error, color = PracticeRed, fontSize = 12.sp)
        }
        Button(
            onClick = onStart,
            enabled = installed,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PracticeGold,
                contentColor = Color(0xFF17130A),
                disabledContainerColor = PracticeLine,
                disabledContentColor = PracticeMuted
            )
        ) {
            Text("Comenzar partida", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TimeControlPicker(
    selected: TimeControlOption,
    options: List<TimeControlOption>,
    onSelected: (TimeControlOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(selected.label, color = PracticeGold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Cambiar", color = PracticeMuted, fontSize = 11.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PracticeColorOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFF292313) else Color.Transparent,
            contentColor = if (selected) PracticeGold else PracticeMuted
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) PracticeGold else PracticeLine
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (label == "Aleatorio") {
                Icon(Icons.Outlined.Shuffle, null, tint = if (selected) PracticeGold else PracticeMuted, modifier = Modifier.size(16.dp).padding(end = 4.dp))
            }
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun PracticeHeader(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    enabled: Boolean,
    strengthPercent: Int,
    timeLabel: String,
    onOpenSettings: () -> Unit,
    profilePhoto: String,
    playerColor: ChessColor?
) {
    val avatar = remember(profilePhoto) {
        try {
            profilePhoto.substringAfter("base64,", profilePhoto).takeIf { it.isNotBlank() }?.let {
                val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("← Inicio", color = PracticeMuted, fontSize = 12.sp)
        }
        // Foto del usuario + icono Stockfish + nombre
        Column(Modifier.weight(1f).padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Foto del usuario
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(PracticeCard, CircleShape)
                        .border(1.5.dp, PracticeGold.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null) {
                        Image(
                            painter = BitmapPainter(avatar),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("T", color = PracticeGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Icono Stockfish
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                        .background(PracticeGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(com.bchess.android.R.drawable.stockfish_fish),
                        contentDescription = "Stockfish",
                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Column {
                    Text("Practicar", color = PracticeText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${strengthLevelName(strengthPercent)} · $strengthPercent% · ${if (playerColor == null) "Aleatorio" else playerColor.toString().uppercase()} · $timeLabel",
                        color = PracticeMuted, fontSize = 10.5.sp,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Botón ajustes (volver a configuración)
        IconButton(onClick = onOpenSettings, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Configuración",
                tint = PracticeGold,
                modifier = Modifier.size(24.dp)
            )
        }
        TextButton(onClick = { /* reiniciar */ }, enabled = enabled, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Reiniciar", color = PracticeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PracticeClockBar(clock: ClockState, timeControl: TimeControlOption, sideToMove: ChessColor) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PracticeClockTile(
            label = "BLANCAS",
            time = if (timeControl.timeMs == 0L) "Ilimitado" else formatClock(clock.whiteMs),
            active = sideToMove == ChessColor.WHITE,
            modifier = Modifier.weight(1f)
        )
        PracticeClockTile(
            label = "NEGRAS",
            time = if (timeControl.timeMs == 0L) "Ilimitado" else formatClock(clock.blackMs),
            active = sideToMove == ChessColor.BLACK,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PracticeClockTile(label: String, time: String, active: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(PracticeCard, RoundedCornerShape(10.dp))
            .border(1.dp, if (active) PracticeGold else PracticeLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = PracticeMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(time, color = if (active) PracticeGold else PracticeText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PracticeReopenResultButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PracticeGold, contentColor = Color(0xFF17130A))
    ) {
        Text("Ver resultado de la partida", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

@Composable
private fun PracticeSidePicker(onDismiss: () -> Unit, onChoose: (ChessColor) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .background(PracticeCard, RoundedCornerShape(18.dp))
                .border(1.dp, PracticeLine, RoundedCornerShape(18.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Elige tu color", color = PracticeText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("La configuración se aplica antes de iniciar la partida.", color = PracticeMuted, fontSize = 12.sp)
            OutlinedButton(onClick = { onChoose(ChessColor.WHITE) }, modifier = Modifier.fillMaxWidth()) {
                Text("Jugar con blancas", color = PracticeText)
            }
            OutlinedButton(onClick = { onChoose(ChessColor.BLACK) }, modifier = Modifier.fillMaxWidth()) {
                Text("Jugar con negras", color = PracticeText)
            }
        }
    }
}

@Composable
private fun StockfishPresentation() {
    Row(
        modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
            .border(1.dp, PracticeLine, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(com.bchess.android.R.drawable.stockfish_fish),
            contentDescription = "Stockfish",
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Stockfish", color = PracticeText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Tu rival de práctica. Ajusta su fuerza de 0% a 100%.",
                color = PracticeMuted, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EngineStatusRow(
    installState: EngineInstallState,
    installed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
            .border(1.dp, PracticeLine, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar del motor sobre el tablero
        Image(
            painter = painterResource(com.bchess.android.R.drawable.stockfish_fish),
            contentDescription = "Stockfish",
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(7.dp).background(if (installed) PracticeGreen else PracticeMuted, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(
            when (installState) {
                EngineInstallState.Ready -> "Motor listo"
                is EngineInstallState.Error -> installState.message
                EngineInstallState.Idle -> "Stockfish integrado"
            },
            color = PracticeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}

@Composable
private fun PracticeLevel(strengthPercent: Int, onStrengthChange: (Int) -> Unit, enabled: Boolean) {
    val levelName = strengthLevelName(strengthPercent)
    val bandIndex = strengthBandIndex(strengthPercent)
    Column(
        modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
            .border(1.dp, PracticeLine, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Fuerza del motor", color = PracticeText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "$levelName · ${strengthBandRange(strengthPercent)}",
                    color = PracticeMuted, fontSize = 10.sp
                )
            }
            Text("$strengthPercent%", color = PracticeGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Slider(
            value = strengthPercent.toFloat(),
            onValueChange = { onStrengthChange(it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 99,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = PracticeGold,
                activeTrackColor = PracticeGold,
                inactiveTrackColor = PracticeLine
            )
        )
        // Escala de niveles: Principiante es solo 0-15
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(STRENGTH_BANDS.size) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index <= bandIndex) PracticeGold.copy(alpha = 0.85f) else PracticeLine
                        )
                )
            }
        }
        Text(
            "0% · Principiante  →  100% · Muy difícil",
            color = PracticeMuted, fontSize = 9.5.sp
        )
    }
}

/** Índice de la banda de nivel (0=Principiante … 5=Muy difícil). */
private fun strengthBandIndex(percent: Int): Int = when (percent) {
    in 0..15 -> 0
    in 16..30 -> 1
    in 31..50 -> 2
    in 51..70 -> 3
    in 71..85 -> 4
    else -> 5
}

private val STRENGTH_BANDS = listOf(
    "Principiante" to 0..15,
    "Aficionado" to 16..30,
    "Intermedio" to 31..50,
    "Avanzado" to 51..70,
    "Experto" to 71..85,
    "Muy difícil" to 86..100
)

private fun strengthBandRange(percent: Int): String =
    STRENGTH_BANDS.firstOrNull { percent in it.second }?.second?.let { "${it.first}-${it.last}%" } ?: ""

/** Interruptor de subida progresiva de nivel con progreso de victorias. */
@Composable
private fun PracticeProgressive(
    enabled: Boolean,
    wins: Int,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (enabled) PracticeGold.copy(alpha = 0.55f) else PracticeLine,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Subida progresiva", color = PracticeText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "Sube $PROGRESSIVE_STEP% de nivel tras $PROGRESSIVE_WINS_NEEDED victorias con el nivel actual",
                    color = PracticeMuted, fontSize = 10.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PracticeGold,
                    checkedTrackColor = PracticeGold.copy(alpha = 0.35f),
                    uncheckedThumbColor = PracticeMuted,
                    uncheckedTrackColor = PracticeLine
                )
            )
        }
        if (enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LinearProgressIndicator(
                    progress = { (wins.toFloat() / PROGRESSIVE_WINS_NEEDED).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = PracticeGold,
                    trackColor = PracticeLine
                )
                Text(
                    "Victorias en el nivel actual: $wins/$PROGRESSIVE_WINS_NEEDED",
                    color = PracticeGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PracticeBoard(
    modifier: Modifier,
    snapshot: com.bchess.android.core.BoardSnapshot,
    selection: BoardSelection,
    onSquareTap: (Int) -> Unit,
    boardTheme: BoardTheme,
    pieceSet: PieceSet,
    showCoordinates: Boolean,
    playerColor: ChessColor?,
    moveIndicators: Boolean,
    highlightLastMove: Boolean = false
) {
    val perspective = playerColor ?: ChessColor.WHITE
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        ChessBoard(
            snapshot = snapshot,
            perspective = perspective,
            selection = selection,
            onSquareTap = onSquareTap,
            modifier = Modifier.size(minOf(maxWidth, maxHeight)),
            theme = boardTheme,
            pieceSet = pieceSet,
            showMoveIndicators = moveIndicators,
            showCoordinates = showCoordinates
        )
    }
}

@Composable
private fun PracticeGameStatus(
    gameEnd: GameEnd?,
    thinking: Boolean,
    sideToMove: ChessColor,
    playerColor: ChessColor?,
    error: String?
) {
    val perspective = playerColor ?: ChessColor.WHITE
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PracticeCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeLine)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        gameEnd != null -> gameEnd.message
                        thinking -> "Stockfish está pensando…"
                        sideToMove == perspective -> if (perspective == ChessColor.WHITE) {
                            "Tu turno · juegas con blancas"
                        } else {
                            "Tu turno · juegas con negras"
                        }
                        else -> "Turno de Stockfish"
                    },
                    color = if (gameEnd != null) PracticeGold else PracticeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (thinking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PracticeGold)
            }
            if (error != null) {
                Text(error, color = PracticeRed, fontSize = 10.sp, maxLines = 2)
            }
        }
    }
}
