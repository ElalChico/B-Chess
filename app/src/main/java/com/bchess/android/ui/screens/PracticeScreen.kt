package com.bchess.android.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.SkipNext
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
import com.bchess.android.core.ChessEngine
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.core.GameEnd
import com.bchess.android.core.GameEndReason
import com.bchess.android.core.Move
import com.bchess.android.core.PieceType
import com.bchess.android.data.AppPreferences
import com.bchess.android.data.BoardTheme
import com.bchess.android.data.ClockManager
import com.bchess.android.data.ClockState
import com.bchess.android.data.EngineInstallState
import com.bchess.android.data.GAME_TYPE_PRACTICE
import com.bchess.android.data.GameHistoryStore
import com.bchess.android.data.GameRecord
import com.bchess.android.data.LocalChessEngine
import com.bchess.android.data.PieceSet
import com.bchess.android.data.PredefinedControls
import com.bchess.android.data.SoundManager
import com.bchess.android.data.SoundType
import com.bchess.android.data.TimeControlOption
import com.bchess.android.data.formatClock
import com.bchess.android.data.strengthLevelName
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
    val sanHistory = remember { mutableStateListOf<String>() }
    val uciHistory = remember { mutableStateListOf<String>() }
    var snapshot by remember { mutableStateOf(engine.snapshot) }
    var selection by remember { mutableStateOf(BoardSelection(null)) }
    var strengthPercent by remember { mutableStateOf(prefs.strengthPercent) }
    var progressiveEnabled by remember { mutableStateOf(prefs.progressiveLevel) }
    var progressiveWins by remember { mutableStateOf(prefs.progressiveWins) }
    var gameEndCount by remember { mutableStateOf(0) }
    var playerColor by remember { mutableStateOf<ChessColor?>(ChessColor.WHITE) }
    var gameStarted by remember { mutableStateOf(false) }
    var forceSetup by remember { mutableStateOf(false) }
    var premoves by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var premoveMode by remember { mutableStateOf(false) }
    var saveHistory by remember { mutableStateOf(true) }
    var paused by remember { mutableStateOf(false) }
    var endSaved by remember { mutableStateOf(false) }

    fun clearSelection() {
        selection = BoardSelection(null, lastMove = selection.lastMove, checkKingSquare = selection.checkKingSquare)
        premoves = emptyList()
    }

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
    LaunchedEffect(gameStarted, gameEnd, paused) {
        onGameStateChange?.invoke(gameStarted && gameEnd == null && !paused)
    }

    var lastMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endDialogDismissed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val playerName = prefs.profileName

    fun buildCurrentRecord(end: GameEnd): GameRecord {
        val currentColor = playerColor ?: ChessColor.WHITE
        val result = when (end.winner) {
            null -> "1/2-1/2"
            ChessColor.WHITE -> "1-0"
            ChessColor.BLACK -> "0-1"
        }
        val localWon = end.winner?.let { it == currentColor }
        return GameRecord(
            opponent = "Stockfish",
            playerName = playerName,
            result = result,
            localWon = localWon,
            localColorWhite = currentColor == ChessColor.WHITE,
            timeLabel = timeControl.label,
            movesSan = sanHistory.toList(),
            pgn = "",
            finalFen = engine.toFEN(),
            uciMoves = uciHistory.toList(),
            gameType = GAME_TYPE_PRACTICE,
            endReason = end.reason.name,
            saveMode = if (saveHistory) "auto" else "never"
        )
    }

    fun saveEndIfNeeded(end: GameEnd) {
        if (endSaved) return
        endSaved = true
        if (saveHistory) {
            try {
                GameHistoryStore.append(context, buildCurrentRecord(end))
            } catch (_: Exception) {}
        }
    }

    val clockRef = remember { object { var cm: ClockManager? = null } }
    val clockManager = remember(coroutineScope) {
        ClockManager(
            scope = coroutineScope,
            isHost = true,
            onSyncRequest = { _ -> },
            onTimeout = { loser ->
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    if (gameEnd != null) return@launch
                    val end = GameEnd(
                        winner = loser.opposite(),
                        reason = GameEndReason.TIMEOUT,
                        message = if (loser == playerColor) "Te quedaste sin tiempo" else "Stockfish se quedó sin tiempo"
                    )
                    gameEnd = end
                    clockRef.cm?.finish()
                    sound?.play(if (loser == playerColor) SoundType.LOSE else SoundType.WIN)
                    saveEndIfNeeded(end)
                    gameEndCount++
                }
            }
        )
    }
    clockRef.cm = clockManager
    val clock by clockManager.state.collectAsState()

    DisposableEffect(engineManager, clockManager) {
        onDispose { clockManager.stop() }
    }

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

    fun resetGame() {
        endDialogDismissed = false
        endSaved = false
        premoves = emptyList()
        paused = false
        clockManager.stop()
        clockManager.setPaused(false)
        clockManager.configure(timeControl.timeMs, timeControl.incrementMs)
        engine.reset()
        sanHistory.clear()
        uciHistory.clear()
        snapshot = engine.snapshot
        selection = BoardSelection(null)
        gameEnd = null
        lastMove = null
        error = null
        thinking = false
    }

    fun finishWith(end: GameEnd) {
        if (gameEnd != null) return
        gameEnd = end
        clockManager.finish()
        paused = false
        sound?.play(
            when {
                end.reason == GameEndReason.CHECKMATE && end.winner == playerColor -> SoundType.WIN
                end.reason == GameEndReason.CHECKMATE -> SoundType.LOSE
                end.winner == playerColor -> SoundType.WIN
                end.winner != null -> SoundType.LOSE
                else -> SoundType.DRAW
            }
        )
        saveEndIfNeeded(end)
        gameEndCount++
    }

    fun refreshAfterMove(move: Move) {
        snapshot = engine.snapshot
        lastMove = move.from to move.to
        val checkedKing = if (checkSignal && engine.isInCheck(engine.sideToMove())) {
            engine.findKing(engine.sideToMove())
        } else null
        selection = BoardSelection(null, lastMove = lastMove, checkKingSquare = checkedKing)

        val current = playerColor
        if (current != null && engine.sideToMove() == current && engine.isInCheck(current)) {
            premoves = emptyList()
        }
        val detected = engine.detectEnd()
        if (detected != null) {
            finishWith(detected)
        }
    }

    fun playMoveSound(captured: Boolean, causedEnd: GameEnd?) {
        when {
            causedEnd?.reason == GameEndReason.CHECKMATE -> sound?.play(SoundType.CHECKMATE)
            causedEnd != null -> sound?.play(SoundType.DRAW)
            engine.isInCheck(engine.sideToMove()) -> sound?.play(SoundType.CHECK)
            captured -> sound?.play(SoundType.CAPTURE)
        }
        val winner = causedEnd?.winner
        if (winner != null) {
            sound?.play(if (winner == playerColor) SoundType.WIN else SoundType.LOSE)
        }
    }

    var requestEngineMoveRef: (() -> Unit)? = null
    var executePremoveIfAnyRef: (() -> Unit)? = null

    fun startNewGame(side: ChessColor?) {
        if (!engineManager.isInstalled()) {
            error = (installState as? EngineInstallState.Error)?.message ?: "Stockfish no está disponible"
            return
        }
        val resolved = side ?: (if (kotlin.random.Random.nextBoolean()) ChessColor.WHITE else ChessColor.BLACK)
        playerColor = resolved
        forceSetup = false
        resetGame()
        gameStarted = true
        clockManager.start()
        if (resolved == ChessColor.BLACK) requestEngineMoveRef?.invoke()
    }

    requestEngineMoveRef = {
        if (gameEnd != null || !gameStarted || paused) Unit else {
            thinking = true
            error = null
            coroutineScope.launch {
                try {
                    val response = engineManager.bestMove(uciHistory.toList(), strengthPercent)
                    if (gameEnd != null || paused) return@launch
                    val move = engine.legalMoves().firstOrNull { it.uci() == response }
                        ?: throw IllegalStateException("Stockfish devolvió un movimiento no válido")
                    val mover = engine.sideToMove()
                    val captured = engine.pieceAt(move.to) != null
                    val san = try { engine.toSAN(move) } catch (_: Exception) { move.uci() }
                    if (!engine.doMove(move)) throw IllegalStateException("No se pudo aplicar el movimiento del motor")
                    uciHistory.add(move.uci())
                    sanHistory.add(if (san.isBlank()) move.uci() else san)
                    clockManager.onMovePlayed(mover)
                    refreshAfterMove(move)
                    playMoveSound(captured, gameEnd)
                    if (gameEnd == null && !paused) executePremoveIfAnyRef?.invoke()
                } catch (e: Exception) {
                    if (gameEnd == null) error = e.message ?: "No se pudo obtener el movimiento del motor"
                } finally {
                    thinking = false
                }
            }
        }
    }

    executePremoveIfAnyRef = {
        if (premoves.isEmpty() || !gameStarted || gameEnd != null || paused) Unit else {
            val currentColor = playerColor
            if (currentColor != null && engine.sideToMove() == currentColor) {
                val (from, to) = premoves.first()
                premoves = premoves.drop(1)
                val piece = engine.pieceAt(from)
                if (piece != null && piece.color == currentColor) {
                    val legalTargets = engine.legalMovesFrom(from).map { it.to }.distinct()
                    if (to in legalTargets) {
                        val move = engine.legalMoves().firstOrNull {
                            it.from == from && it.to == to && (it.promotion == null || it.promotion == PieceType.QUEEN)
                        } ?: engine.legalMoves().firstOrNull { it.from == from && it.to == to }
                        if (move != null) {
                            val captured = engine.pieceAt(move.to) != null
                            val san = try { engine.toSAN(move) } catch (_: Exception) { move.uci() }
                            if (engine.doMove(move)) {
                                uciHistory.add(move.uci())
                                sanHistory.add(if (san.isBlank()) move.uci() else san)
                                clockManager.onMovePlayed(currentColor)
                                snapshot = engine.snapshot
                                selection = BoardSelection(null, lastMove = from to to, checkKingSquare = null)
                                refreshAfterMove(move)
                                playMoveSound(captured, gameEnd)
                                if (gameEnd == null && !paused) requestEngineMoveRef?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }

    fun togglePause() {
        if (!gameStarted || gameEnd != null) return
        val next = !paused
        paused = next
        clockManager.setPaused(next)
        sound?.play(SoundType.PAUSE)
    }

    fun offerDraw() {
        if (!gameStarted || gameEnd != null) return
        finishWith(GameEnd(null, GameEndReason.DRAW_AGREED, "Tablas acordadas"))
    }

    fun resign() {
        if (!gameStarted || gameEnd != null) return
        val winner = (playerColor ?: ChessColor.WHITE).opposite()
        finishWith(GameEnd(winner, GameEndReason.RESIGNATION, "Te has rendido"))
    }

    fun restartSame() {
        val side = playerColor ?: ChessColor.WHITE
        startNewGame(side)
    }

    fun rematchRandom() {
        startNewGame(if (kotlin.random.Random.nextBoolean()) ChessColor.WHITE else ChessColor.BLACK)
    }

    fun prepareNewGame(side: ChessColor?) {
        playerColor = side
        gameStarted = false
        resetGame()
    }

    fun onSquareTap(square: Int) {
        val currentColor = playerColor ?: return
        if (paused || thinking || gameEnd != null) return
        val myTurn = gameStarted && engine.sideToMove() == currentColor
        if (!gameStarted) return
        val premoveModeActive = premoveMode || !myTurn

        if (premoveModeActive) {
            if (!engineManager.isInstalled()) return
            val piece = engine.pieceAt(square)
            val selected = selection
            if (piece?.color == playerColor) {
                selection = BoardSelection(
                    from = square,
                    legalTargets = engine.legalMovesFrom(square).map { it.to }.distinct(),
                    lastMove = lastMove
                )
                return
            }
            val from = selected.from
            if (from != null && square in selected.legalTargets) {
                premoves = premoves + (from to square)
                selection = BoardSelection(null, lastMove = lastMove)
            }
            return
        }

        if (!engineManager.isInstalled()) {
            error = (installState as? EngineInstallState.Error)?.message ?: "Stockfish no está disponible"
            return
        }
        val piece = engine.pieceAt(square)
        val selected = selection
        if (piece?.color == currentColor) {
            selection = BoardSelection(
                from = square,
                legalTargets = engine.legalMovesFrom(square).map { it.to }.distinct(),
                lastMove = lastMove
            )
            error = null
            return
        }
        val from = selected.from
        if (from != null && square in selected.legalTargets) {
            val move = engine.legalMoves().firstOrNull {
                it.from == from && it.to == square && (it.promotion == null || it.promotion == PieceType.QUEEN)
            } ?: engine.legalMoves().firstOrNull { it.from == from && it.to == square } ?: run {
                selection = BoardSelection(null, lastMove = lastMove)
                return
            }
            val captured = engine.pieceAt(move.to) != null
            val san = try { engine.toSAN(move) } catch (_: Exception) { move.uci() }
            if (!engine.doMove(move)) {
                error = "No se pudo aplicar el movimiento"
                return
            }
            uciHistory.add(move.uci())
            sanHistory.add(if (san.isBlank()) move.uci() else san)
            clockManager.onMovePlayed(currentColor)
            refreshAfterMove(move)
            playMoveSound(captured, gameEnd)
            if (gameEnd == null && !paused) requestEngineMoveRef?.invoke()
            if (gameEnd == null && !paused) executePremoveIfAnyRef?.invoke()
        } else {
            if (selected.from != null) sound?.play(SoundType.ILLEGAL)
            selection = BoardSelection(null, lastMove = lastMove)
        }
    }

    if (!gameStarted || forceSetup) {
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
            gameInProgress = gameStarted,
            onBackToGame = { forceSetup = false },
            error = error,
            saveHistory = saveHistory,
            onSaveHistoryChange = { saveHistory = it }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(PracticeBg).padding(10.dp)
        ) {
            val landscape = maxWidth > maxHeight
            val myColor = playerColor ?: ChessColor.WHITE
            val oppColor = myColor.opposite()
            val unlimited = timeControl.timeMs == 0L
            val myTime = if (myColor == ChessColor.WHITE) clock.whiteMs else clock.blackMs
            val oppTime = if (oppColor == ChessColor.WHITE) clock.whiteMs else clock.blackMs
            val myIsLowTime = !unlimited && myTime > 0L && myTime < 10_000L
            val oppIsLowTime = !unlimited && oppTime > 0L && oppTime < 10_000L
            val sideToMove = engine.sideToMove()

            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        Modifier.weight(0.42f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PracticeHeader(
                            enabled = !thinking,
                            strengthPercent = strengthPercent,
                            timeLabel = timeControl.label,
                            onOpenSettings = { forceSetup = true },
                            premoveMode = premoveMode,
                            onTogglePremoveMode = { premoveMode = !premoveMode },
                            premoveCount = premoves.size
                        )
                        PracticeControlRow(
                            paused = paused,
                            gameOver = gameEnd != null,
                            onPause = ::togglePause,
                            onDraw = ::offerDraw,
                            onResign = ::resign,
                            onRestart = ::restartSame,
                            onRematch = ::rematchRandom
                        )
                        Spacer(Modifier.weight(1f))
                        if (gameEnd != null && endDialogDismissed) {
                            PracticeReopenResultButton { endDialogDismissed = false }
                        }
                        PracticeGameStatus(
                            gameEnd = gameEnd,
                            thinking = thinking,
                            sideToMove = sideToMove,
                            playerColor = playerColor,
                            error = error,
                            paused = paused
                        )
                    }
                    Column(
                        Modifier.weight(0.58f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PracticeClockCard(
                            name = "Stockfish",
                            side = oppColor,
                            timeMs = oppTime,
                            unlimited = unlimited,
                            active = sideToMove == oppColor,
                            isLowTime = oppIsLowTime,
                            isStockfish = true,
                            photoBase64 = null
                        )
                        PracticeBoard(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            snapshot = snapshot,
                            selection = selection,
                            onSquareTap = ::onSquareTap,
                            onClearSelection = ::clearSelection,
                            boardTheme = boardTheme,
                            pieceSet = pieceSet,
                            showCoordinates = showCoordinates,
                            playerColor = playerColor,
                            moveIndicators = moveIndicators
                        )
                        PracticeClockCard(
                            name = playerName.ifBlank { "Tú" },
                            side = myColor,
                            timeMs = myTime,
                            unlimited = unlimited,
                            active = sideToMove == myColor,
                            isLowTime = myIsLowTime,
                            isStockfish = false,
                            photoBase64 = prefs.profilePhotoBase64
                        )
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PracticeHeader(
                        enabled = !thinking,
                        strengthPercent = strengthPercent,
                        timeLabel = timeControl.label,
                        onOpenSettings = { forceSetup = true },
                        premoveMode = premoveMode,
                        onTogglePremoveMode = { premoveMode = !premoveMode },
                        premoveCount = premoves.size
                    )
                    PracticeClockCard(
                        name = "Stockfish",
                        side = oppColor,
                        timeMs = oppTime,
                        unlimited = unlimited,
                        active = sideToMove == oppColor,
                        isLowTime = oppIsLowTime,
                        isStockfish = true,
                        photoBase64 = null
                    )
                    PracticeBoard(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        snapshot = snapshot,
                        selection = selection,
                        onSquareTap = ::onSquareTap,
                        onClearSelection = ::clearSelection,
                        boardTheme = boardTheme,
                        pieceSet = pieceSet,
                        showCoordinates = showCoordinates,
                        playerColor = playerColor,
                        moveIndicators = moveIndicators
                    )
                    PracticeClockCard(
                        name = playerName.ifBlank { "Tú" },
                        side = myColor,
                        timeMs = myTime,
                        unlimited = unlimited,
                        active = sideToMove == myColor,
                        isLowTime = myIsLowTime,
                        isStockfish = false,
                        photoBase64 = prefs.profilePhotoBase64
                    )
                    PracticeControlRow(
                        paused = paused,
                        gameOver = gameEnd != null,
                        onPause = ::togglePause,
                        onDraw = ::offerDraw,
                        onResign = ::resign,
                        onRestart = ::restartSame,
                        onRematch = ::rematchRandom
                    )
                    if (gameEnd != null && endDialogDismissed) {
                        PracticeReopenResultButton { endDialogDismissed = false }
                    }
                    PracticeGameStatus(
                        gameEnd = gameEnd,
                        thinking = thinking,
                        sideToMove = sideToMove,
                        playerColor = playerColor,
                        error = error,
                        paused = paused
                    )
                }
            }

            if (gameEnd != null && !endDialogDismissed) {
                val end = gameEnd!!
                val record = buildCurrentRecord(end)
                val localName = playerName
                val localC = playerColor ?: ChessColor.WHITE
                GameEndDialog(
                    end = end,
                    isHost = true,
                    tournamentActive = false,
                    tournamentFinished = false,
                    localName = localName,
                    remoteName = "Stockfish",
                    localPhotoBase64 = prefs.profilePhotoBase64,
                    remotePhotoBase64 = null,
                    winnerName = when (end.winner) {
                        null -> null
                        localC -> localName
                        else -> "Stockfish"
                    },
                    localColor = localC,
                    onNewGameSame = ::restartSame,
                    onNewGameRandom = ::rematchRandom,
                    onTournamentNext = { },
                    onBackToRoom = {
                        endDialogDismissed = true
                        gameStarted = false
                        forceSetup = false
                        resetGame()
                    },
                    onReplayPgn = {
                        saveEndIfNeeded(end)
                        onReplay(buildCurrentRecord(end))
                    },
                    onDismiss = { endDialogDismissed = true }
                )
            }
        }
    }
}

@Composable
private fun PracticeControlRow(
    paused: Boolean,
    gameOver: Boolean,
    onPause: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    onRestart: () -> Unit,
    onRematch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onPause,
                enabled = !gameOver,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeLine)
            ) { Text(if (paused) "Reanudar" else "Pausar", color = PracticeText, fontSize = 12.sp) }
            OutlinedButton(
                onClick = onDraw,
                enabled = !gameOver,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeLine)
            ) { Text("Tablas", color = PracticeText, fontSize = 12.sp) }
            OutlinedButton(
                onClick = onResign,
                enabled = !gameOver,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeRed.copy(alpha = 0.6f))
            ) { Text("Rendirse", color = PracticeRed, fontSize = 12.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PracticeGold.copy(alpha = 0.6f))
            ) { Text("Reiniciar", color = PracticeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = onRematch,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PracticeGold, contentColor = Color(0xFF17130A))
            ) { Text("Rematch", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
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
    gameInProgress: Boolean,
    onBackToGame: () -> Unit,
    error: String?,
    saveHistory: Boolean,
    onSaveHistoryChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(PracticeBg).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = if (gameInProgress) onBackToGame else onBack,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(if (gameInProgress) "← Volver" else "← Inicio", color = PracticeMuted, fontSize = 12.sp)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("Configuración de Stockfish", color = PracticeText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if (gameInProgress) "Partida en curso" else "Contra Stockfish", color = PracticeMuted, fontSize = 11.sp)
            }
        }
        if (gameInProgress) {
            Button(
                onClick = onBackToGame,
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PracticeGold, contentColor = Color(0xFF17130A)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Text("Volver a la partida", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
        EngineStatusRow(installState = installState, installed = installed)
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
        Column(
            modifier = Modifier.fillMaxWidth().background(PracticeCard, RoundedCornerShape(12.dp))
                .border(1.dp, PracticeLine, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Guardar en historial", color = PracticeText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (saveHistory) "Se guardará automáticamente al terminar" else "Esta partida no se guardará",
                        color = PracticeMuted, fontSize = 10.sp
                    )
                }
                Switch(
                    checked = saveHistory,
                    onCheckedChange = onSaveHistoryChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PracticeGold,
                        checkedTrackColor = PracticeGold.copy(alpha = 0.35f),
                        uncheckedThumbColor = PracticeMuted,
                        uncheckedTrackColor = PracticeLine
                    )
                )
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
    enabled: Boolean,
    strengthPercent: Int,
    timeLabel: String,
    onOpenSettings: () -> Unit,
    premoveMode: Boolean,
    onTogglePremoveMode: () -> Unit,
    premoveCount: Int
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Practicar", color = PracticeText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${strengthLevelName(strengthPercent)} · $strengthPercent% · $timeLabel",
                color = PracticeMuted,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onTogglePremoveMode, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Outlined.SkipNext,
                contentDescription = if (premoveMode) "Premueve activado" else "Premueve desactivado",
                tint = if (premoveMode) Color(0xFF3DDC97) else PracticeMuted,
                modifier = Modifier.size(24.dp)
            )
        }
        if (premoveCount > 0) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF1A2E1F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("$premoveCount", color = Color(0xFF3DDC97), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onOpenSettings, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Configuración",
                tint = PracticeGold,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PracticeClockCard(
    name: String,
    side: ChessColor,
    timeMs: Long,
    unlimited: Boolean,
    active: Boolean,
    isLowTime: Boolean,
    isStockfish: Boolean,
    photoBase64: String?,
    modifier: Modifier = Modifier
) {
    val sideLabel = if (side == ChessColor.WHITE) "BLANCAS" else "NEGRAS"
    val avatarBitmap = remember(photoBase64, isStockfish) {
        if (isStockfish) null else try {
            photoBase64?.substringAfter("base64,", photoBase64)?.takeIf { it.isNotBlank() }?.let {
                val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    val bgColor = when {
        isLowTime -> Color(0xFF2A1A1A)
        active -> Color(0xFF1A1F28)
        else -> PracticeCard
    }
    val borderColor = when {
        isLowTime -> PracticeRed
        active -> PracticeGreen
        else -> PracticeLine
    }
    val nameColor = when {
        isLowTime -> PracticeRed
        active -> PracticeGreen
        else -> PracticeText
    }
    val timeColor = when {
        isLowTime -> PracticeRed
        active -> PracticeGreen
        else -> PracticeText
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(
                1.5.dp,
                borderColor,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isStockfish) PracticeGold.copy(alpha = 0.12f) else Color(0xFF15251D))
                .border(
                    1.5.dp,
                    if (isStockfish) PracticeGold.copy(alpha = 0.6f) else borderColor.copy(alpha = 0.7f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isStockfish) {
                Image(
                    painter = painterResource(com.bchess.android.R.drawable.stockfish_fish),
                    contentDescription = "Stockfish",
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (avatarBitmap != null) {
                Image(
                    painter = BitmapPainter(avatarBitmap),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = (name.firstOrNull() ?: 'T').uppercaseChar().toString(),
                    color = nameColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                color = nameColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                sideLabel,
                color = PracticeMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }

        Text(
            text = if (unlimited) "∞" else formatClock(timeMs),
            color = timeColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
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

private fun strengthBandIndex(percent: Int): Int = when (percent) {
    in 0..15 -> 0
    in 16..27 -> 1
    in 28..50 -> 2
    in 51..70 -> 3
    in 71..85 -> 4
    else -> 5
}

private val STRENGTH_BANDS = listOf(
    "Principiante" to 0..15,
    "Aficionado" to 16..27,
    "Intermedio" to 28..50,
    "Avanzado" to 51..70,
    "Experto" to 71..85,
    "Muy difícil" to 86..100
)

private fun strengthBandRange(percent: Int): String =
    STRENGTH_BANDS.firstOrNull { percent in it.second }?.second?.let { "${it.first}-${it.last}%" } ?: ""

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
    onClearSelection: () -> Unit,
    boardTheme: BoardTheme,
    pieceSet: PieceSet,
    showCoordinates: Boolean,
    playerColor: ChessColor?,
    moveIndicators: Boolean,
    highlightLastMove: Boolean = false
) {
    val perspective = playerColor ?: ChessColor.WHITE
    BoxWithConstraints(modifier.clickable { onClearSelection() }, contentAlignment = Alignment.Center) {
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
    error: String?,
    paused: Boolean = false
) {
    val perspective = playerColor ?: ChessColor.WHITE
    val message = when {
        gameEnd != null -> gameEnd.message
        paused -> "Partida en pausa"
        thinking -> "Stockfish está pensando…"
        sideToMove == perspective -> null
        else -> "Turno de Stockfish"
    }
    if (message == null && error == null) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PracticeCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PracticeLine)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            if (message != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message,
                        color = if (gameEnd != null) PracticeGold else PracticeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (thinking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PracticeGold)
                }
            }
            if (error != null) {
                Text(error, color = PracticeRed, fontSize = 10.sp, maxLines = 2)
            }
        }
    }
}
