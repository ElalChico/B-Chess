package com.bchess.android

import android.app.Activity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bchess.android.data.AppLanguage
import com.bchess.android.data.AppPreferences
import com.bchess.android.data.BtState
import com.bchess.android.data.GameHistoryStore
import com.bchess.android.data.GameRecord
import com.bchess.android.data.StockfishEngineManager
import com.bchess.android.data.UpdateManager
import com.bchess.android.data.UpdateState
import com.bchess.android.data.discoverableIntent
import com.bchess.android.data.requiredPermissions
import com.bchess.android.ui.GameViewModel
import com.bchess.android.ui.screens.ConnectionFlowScreen
import com.bchess.android.ui.screens.GameScreen
import com.bchess.android.ui.screens.HomeScreen
import com.bchess.android.ui.screens.LobbyScreen
import com.bchess.android.ui.screens.PracticeScreen
import com.bchess.android.ui.screens.ReplayScreen
import com.bchess.android.ui.screens.RivalProfileDialog
import com.bchess.android.ui.screens.RoomScreen
import com.bchess.android.ui.screens.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { HOME, CONNECT, PROFILE, GAME, SETTINGS, REPLAY, ROOM, PRACTICE }

private val NavGold = Color(0xFFE2B94E)
private val NavMuted = Color(0xFF8B96A5)
private val NavBg = Color(0xFF10141A)
private val NavLine = Color(0xFF252E39)

class MainActivity : ComponentActivity() {

    private lateinit var vm: GameViewModel
    private lateinit var prefs: AppPreferences

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.all { it }
        if (!granted) android.util.Log.w("Perms", "Some BT permissions denied: $results")
    }

    private val discoverableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        try { vm.bluetooth.hostListen() } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(applicationContext) as T
        }
        vm = ViewModelProvider(this, factory)[GameViewModel::class.java]
        vm.init()
        requestPermissionsIfNeeded()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF090C10), surface = Color(0xFF121A22), surfaceVariant = Color(0xFF1B242D),
                    primary = Color(0xFFE2B94E), onPrimary = Color(0xFF131313),
                    onBackground = Color(0xFFEFF3F8), onSurface = Color(0xFFEFF3F8),
                    onSurfaceVariant = Color(0xFF9CA8B4), outline = Color(0xFF2C3742)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0D12),
                                Color(0xFF111821),
                                Color(0xFF0B0D10)
                            )
                        )
                    ),
                    color = Color.Transparent
                ) {
                    ChessApp(vm, prefs, this@MainActivity, permissionLauncher, discoverableLauncher)
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val perms = requiredPermissions()
        val toAsk = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (toAsk.isNotEmpty() && Build.VERSION.SDK_INT >= 23) permissionLauncher.launch(toAsk.toTypedArray())
        try {
            @Suppress("DEPRECATION")
            val a = BluetoothAdapter.getDefaultAdapter()
            if (a != null && !a.isEnabled) { try { @Suppress("DEPRECATION") a.enable() } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { vm.onClearedCleanup() } catch (_: Exception) {}
    }

    @Composable
    fun ChessApp(
        vm: GameViewModel,
        prefs: AppPreferences,
        activity: Activity,
        permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
        discoverableLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        var screen by remember { mutableStateOf(Screen.HOME) }
        var isHostMode by remember { mutableStateOf(false) }
        var profileNextScreen by remember { mutableStateOf(Screen.HOME) }
        var connectReturnScreen by remember { mutableStateOf(Screen.HOME) }
        var fullscreen by remember { mutableStateOf(false) }
        var history by remember { mutableStateOf(GameHistoryStore.load(activity)) }
        var replayRecord by remember { mutableStateOf<GameRecord?>(null) }
        var showRivalProfile by remember { mutableStateOf(false) }
        var returnToPracticeFromSettings by remember { mutableStateOf(false) }
        val updateScope = remember { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

        LaunchedEffect(screen) { if (screen == Screen.PROFILE) history = GameHistoryStore.load(activity) }

        val ui by vm.ui.collectAsState()
        val btState by vm.bluetooth.state.collectAsState()
        val devices by vm.bluetooth.devices.collectAsState()
        val btError by vm.bluetooth.lastError.collectAsState()
        val remoteProfile by vm.remoteProfile.collectAsState()
        val isEn = prefs.language == AppLanguage.EN
        val startHostListening: () -> Unit = {
            vm.bluetooth.setVisibleDeviceName("${prefs.profileName} · Anfitrión")
            try {
                discoverableLauncher.launch(discoverableIntent())
            } catch (_: Exception) {
                try { vm.bluetooth.hostListen() } catch (_: Exception) {}
            }
        }
        val startGuestScanning: () -> Unit = {
            vm.bluetooth.setVisibleDeviceName("${prefs.profileName} · Invitado")
            vm.bluetooth.startDiscovery()
        }
        val continueFromProfile: () -> Unit = {
            prefs.completeProfile()
            when (profileNextScreen) {
                Screen.ROOM -> {
                    screen = Screen.ROOM
                    if (isHostMode) startHostListening()
                }
                Screen.CONNECT -> {
                    screen = Screen.CONNECT
                    if (isHostMode) startHostListening() else startGuestScanning()
                }
                Screen.GAME -> screen = Screen.GAME
                Screen.SETTINGS -> screen = Screen.SETTINGS
                Screen.PRACTICE -> screen = Screen.PRACTICE
                else -> screen = Screen.HOME
            }
            profileNextScreen = Screen.HOME
        }

        LaunchedEffect(prefs.soundEnabled, prefs.soundVolume) {
            try { vm.sound.setEnabled(prefs.soundEnabled); vm.sound.setVolume(prefs.soundVolume) } catch (_: Exception) {}
        }
        LaunchedEffect(prefs.soundPack) {
            try { vm.sound.setPack(prefs.soundPack) } catch (_: Exception) {}
        }
        LaunchedEffect(prefs.profileName) { vm.setLocalName(prefs.profileName) }

        LaunchedEffect(fullscreen) {
            try {
                val controller = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                if (fullscreen) {
                    controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } catch (_: Exception) {}
        }

        LaunchedEffect(btState) {
            if (btState == BtState.CONNECTED) {
                vm.sendHello()
                vm.sendProfile()
                if (ui.lobby.isHost) vm.sendLobbyConfig()
                if (screen == Screen.CONNECT) screen = Screen.ROOM
            }
        }
        LaunchedEffect(ui.lobby.gameStarted) {
            if (ui.lobby.gameStarted && (screen == Screen.PROFILE || screen == Screen.ROOM)) screen = Screen.GAME
        }

        val updateManager = remember { UpdateManager.getInstance(activity) }
        val engineManager = remember { StockfishEngineManager.getInstance(activity) }
        val updateState by updateManager.updateState.collectAsStateWithLifecycle()
        var dismissedUpdateCode by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(Unit) {
            updateScope.launch { updateManager.checkForUpdate() }
        }

        Column(Modifier.fillMaxSize().background(Color(0xFF0B0D10))) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    inSession = btState == BtState.CONNECTED,
                    isGuestSession = btState == BtState.CONNECTED && !ui.lobby.isHost,
                    profileConfigured = prefs.profileConfigured,
                    profileName = prefs.profileName,
                    profilePhotoBase64 = prefs.profilePhotoBase64,
                    clockSummary = if (prefs.defaultTimeMs == 0L) "∞" else "${prefs.defaultTimeMs / 60_000} + ${prefs.defaultIncrementMs / 1_000}",
                    onProfile = {
                        profileNextScreen = Screen.HOME
                        screen = Screen.PROFILE
                    },
                    onCreate = {
                        isHostMode = true
                        vm.setIsHost(true)
                        if (prefs.profileConfigured) {
                            screen = Screen.CONNECT
                            startHostListening()
                        } else {
                            profileNextScreen = Screen.CONNECT
                            screen = Screen.PROFILE
                        }
                    },
                    onSearch = {
                        isHostMode = false
                        vm.setIsHost(false)
                        if (prefs.profileConfigured) {
                            screen = Screen.CONNECT
                            startGuestScanning()
                        } else {
                            profileNextScreen = Screen.CONNECT
                            screen = Screen.PROFILE
                        }
                    },
                    onPractice = {
                        if (prefs.profileConfigured) {
                            screen = Screen.PRACTICE
                        } else {
                            profileNextScreen = Screen.PRACTICE
                            screen = Screen.PROFILE
                        }
                    },
                )

                Screen.CONNECT -> ConnectionFlowScreen(
                    isHost = isHostMode,
                    state = btState,
                    devices = devices,
                    playerName = ui.localName.ifBlank { prefs.profileName },
                    remoteName = ui.remoteName,
                    error = btError,
                    onListen = startHostListening,
                    onSearch = startGuestScanning,
                    onConnect = { addr -> vm.bluetooth.connectTo(addr) },
                    onBack = { vm.disconnect(); screen = Screen.HOME }
                )

                Screen.PROFILE -> LobbyScreen(
                    lobby = ui.lobby, localName = ui.localName, remoteName = ui.remoteName,
                    photoBase64 = prefs.profilePhotoBase64, history = history,
                    profileSetupRequired = !prefs.profileConfigured,
                    remoteProfile = remoteProfile,
                    connected = btState == BtState.CONNECTED,
                    onViewRivalProfile = { showRivalProfile = true },
                    continueLabel = when (profileNextScreen) {
                        Screen.HOME -> if (!prefs.profileConfigured) "Guardar perfil y comenzar" else "Guardar cambios"
                        Screen.CONNECT -> "Guardar perfil y conectar"
                        Screen.ROOM -> "Continuar · configurar partida"
                        else -> "Guardar perfil y continuar"
                    },
                    onContinue = continueFromProfile,
                    onNameChange = { prefs.setProfileName(it); vm.setLocalName(it); vm.sendHello(); vm.sendProfile() },
                    onReplay = { replayRecord = it; screen = Screen.REPLAY },
                    onDeleteGame = { rec -> GameHistoryStore.remove(activity, rec.id); history = GameHistoryStore.load(activity) },
                    onClearHistory = { GameHistoryStore.clear(activity); history = emptyList() },
                    onImportGames = { records ->
                        val imported = GameHistoryStore.importRecords(activity, records)
                        history = GameHistoryStore.load(activity)
                        imported
                    },
                    onPhotoCropped = { prefs.setProfilePhotoBase64(it); vm.sendProfile() },
                    onBack = {
                        screen = profileNextScreen
                        profileNextScreen = Screen.HOME
                    }
                )

                Screen.ROOM -> RoomScreen(
                    lobby = ui.lobby, localName = ui.localName, remoteName = ui.remoteName,
                    localPhotoBase64 = prefs.profilePhotoBase64,
                    remoteProfile = remoteProfile,
                    onViewRivalProfile = { showRivalProfile = true },
                    onTimePick = { timeMs, inc -> vm.setTimeControl(timeMs, inc); if (ui.lobby.isHost) vm.sendLobbyConfig() },
                    onColorPick = { color -> vm.setLocalColor(color) },
                    onRandomColor = { vm.setRandomColor() },
                    onConnect = {
                        isHostMode = true
                        vm.setIsHost(true)
                        startHostListening()
                    },
                    onStartGame = vm::startGameAsHost,
                    onStartTournament = { games -> vm.startTournament(games, ui.lobby.timeMs, ui.lobby.incrementMs) },
                    onBack = { screen = Screen.HOME; vm.disconnect() }
                )

                Screen.REPLAY -> {
                    val rec = replayRecord
                    if (rec != null) ReplayScreen(rec, showCoordinates = prefs.showCoordinates, onBack = { screen = Screen.PROFILE })
                    else LobbyScreen(
                        lobby = ui.lobby, localName = ui.localName, remoteName = ui.remoteName,
                        photoBase64 = prefs.profilePhotoBase64, history = history,
                        remoteProfile = remoteProfile,
                        connected = btState == BtState.CONNECTED,
                        onViewRivalProfile = { showRivalProfile = true },
                        continueLabel = "Volver al perfil",
                        onContinue = { screen = Screen.PROFILE },
                        onNameChange = { prefs.setProfileName(it); vm.setLocalName(it); vm.sendHello(); vm.sendProfile() },
                        onReplay = { replayRecord = it; screen = Screen.REPLAY },
                        onDeleteGame = { r -> GameHistoryStore.remove(activity, r.id); history = GameHistoryStore.load(activity) },
                        onClearHistory = { GameHistoryStore.clear(activity); history = emptyList() },
                        onImportGames = { records ->
                            val imported = GameHistoryStore.importRecords(activity, records)
                            history = GameHistoryStore.load(activity)
                            imported
                        },
                        onPhotoCropped = { prefs.setProfilePhotoBase64(it); vm.sendProfile() },
                        onBack = { screen = connectReturnScreen }
                    )
                }

                Screen.GAME -> GameScreen(
                    ui = ui, isFullscreen = fullscreen, boardTheme = prefs.boardTheme,
                    pieceSet = prefs.pieceSet,
                    moveIndicators = prefs.moveIndicators,
                    showCoordinates = prefs.showCoordinates,
                    localPhotoBase64 = prefs.profilePhotoBase64,
                    remoteProfile = remoteProfile,
                    onViewRivalProfile = { showRivalProfile = true },
                    onToggleFullscreen = { fullscreen = !fullscreen },
                    onSquareTap = vm::onSquareTap,
                    onTogglePause = vm::togglePause,
                    onOfferDraw = vm::offerDraw,
                    onAcceptDraw = { vm.respondDraw(true) },
                    onRejectDraw = { vm.respondDraw(false) },
                    onResign = vm::resign,
                    onPromotion = vm::confirmPromotion,
                    onCancelPromotion = vm::cancelPromotion,
                    onToggleSound = { prefs.setSoundEnabled(!prefs.soundEnabled) },
                    onDisconnect = { fullscreen = false; vm.disconnect(); screen = Screen.HOME },
                    onNewGameSame = { vm.requestNewGame("same") },
                    onNewGameRandom = { vm.requestNewGame("random") },
                    onTournamentNext = { vm.requestNewGame("tournament") },
                    onBackToRoom = { vm.backToRoom() },
                    onGameEndDismiss = { },
                    onNewTournament = { screen = Screen.ROOM },
                    onReplayPgn = {
                        // Get the last finished game record for replay
                        val history = GameHistoryStore.load(activity)
                        if (history.isNotEmpty()) {
                            replayRecord = history[0]
                            screen = Screen.REPLAY
                        }
                    },
                )

                Screen.SETTINGS -> SettingsScreen(
                    prefs = prefs,
                    onDisconnectBt = { try { vm.disconnect() } catch (_: Exception) {} },
                    onOpenBtSettings = {
                        try { activity.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) } catch (_: Exception) {}
                    },
                    onExit = { activity.finish() },
                    activity = activity,
                    onBackToPractice = if (returnToPracticeFromSettings) {
                        {
                            returnToPracticeFromSettings = false
                            screen = Screen.PRACTICE
                        }
                    } else {
                        null
                    }
                )

                Screen.PRACTICE -> PracticeScreen(
                    engineManager = engineManager,
                    boardTheme = prefs.boardTheme,
                    pieceSet = prefs.pieceSet,
                    showCoordinates = prefs.showCoordinates,
                    moveIndicators = prefs.moveIndicators,
                    checkSignal = prefs.checkSignal,
                    initialTimeMs = prefs.defaultTimeMs,
                    initialIncrementMs = prefs.defaultIncrementMs,
                    onBack = { screen = Screen.HOME },
                    onOpenSettings = {
                        returnToPracticeFromSettings = true
                        screen = Screen.SETTINGS
                    },
                    prefs = prefs,
                    onReplay = { record ->
                        replayRecord = record
                        screen = Screen.REPLAY
                    }
                )
            }

            }

            UpdateFooter(
                isEn = isEn,
                currentVersion = updateManager.getCurrentVersion(),
                latestVersion = updateManager.getLatestKnownVersion(),
                updateState = updateState,
                isDeferred = dismissedUpdateCode == (updateState as? UpdateState.UpdateAvailable)?.result?.remoteVersion?.versionCode,
                onDefer = { dismissedUpdateCode = it.remoteVersion.versionCode },
                onUpdate = { update ->
                    dismissedUpdateCode = update.remoteVersion.versionCode
                    update.apkUrl?.let { apkUrl ->
                        updateScope.launch {
                            val result = updateManager.downloadAndInstall(apkUrl)
                            if (result.isFailure) {
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        activity,
                                        "No se pudo instalar la actualización: ${result.exceptionOrNull()?.message ?: "error desconocido"}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            )

            if (!(screen == Screen.GAME && fullscreen) && screen != Screen.CONNECT && screen != Screen.ROOM) {
                BottomNav(current = if (screen == Screen.REPLAY) Screen.PROFILE else screen, isEn = isEn, onSelect = { target ->
                    when (target) {
                        Screen.HOME -> {
                            fullscreen = false
                            screen = Screen.HOME
                        }
                        Screen.PROFILE, Screen.REPLAY -> {
                            fullscreen = false
                            profileNextScreen = when (screen) {
                                Screen.ROOM -> Screen.ROOM
                                Screen.CONNECT -> Screen.CONNECT
                                Screen.GAME -> Screen.GAME
                                Screen.SETTINGS -> Screen.SETTINGS
                                else -> Screen.HOME
                            }
                            screen = Screen.PROFILE
                        }
                        Screen.GAME -> {
                            if (ui.lobby.gameStarted && ui.gameEnd == null && btState == BtState.CONNECTED) {
                                fullscreen = false
                                screen = Screen.GAME
                            }
                        }
                        Screen.SETTINGS -> {
                            fullscreen = false
                            screen = Screen.SETTINGS
                        }
                        else -> { /* ROOM not in bottom nav */ }
                    }
                }, gameEnabled = ui.lobby.gameStarted && ui.gameEnd == null && btState == BtState.CONNECTED)
            }
        }

        if (showRivalProfile && remoteProfile != null) {
            RivalProfileDialog(
                profile = remoteProfile!!,
                onDismiss = { showRivalProfile = false },
                onReplay = { game ->
                    replayRecord = GameRecord(
                        id = "remote-${game.dateMs}-${game.moves}",
                        dateMs = game.dateMs,
                        opponent = game.opponent,
                        result = game.result,
                        localWon = game.won,
                        localColorWhite = game.localColorWhite,
                        timeLabel = game.timeLabel,
                        movesSan = game.movesSan,
                        pgn = game.pgn,
                        finalFen = "",
                        playerName = game.playerName,
                        uciMoves = game.uciMoves
                    )
                    showRivalProfile = false
                    screen = Screen.REPLAY
                }
            )
        }
    }
}

@Composable
private fun UpdateFooter(
    isEn: Boolean,
    currentVersion: com.bchess.android.data.VersionInfo,
    latestVersion: com.bchess.android.data.VersionInfo,
    updateState: UpdateState,
    isDeferred: Boolean,
    onDefer: (com.bchess.android.data.UpdateCheckResult) -> Unit,
    onUpdate: (com.bchess.android.data.UpdateCheckResult) -> Unit
) {
    val available = (updateState as? UpdateState.UpdateAvailable)?.result
    val latest = available?.remoteVersion ?: latestVersion
    val status = when (updateState) {
        UpdateState.Checking -> if (isEn) "Checking for updates…" else "Buscando actualizaciones…"
        is UpdateState.Error -> if (isEn) "Last known" else "Última conocida"
        else -> if (available != null) {
            if (isEn) "Update available" else "Actualización disponible"
        } else {
            if (isEn) "Up to date" else "Al día"
        }
    }
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF111820)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isEn) "Current ${currentVersion.versionName}" else "Actual ${currentVersion.versionName}", color = NavMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("$status · ${latest.versionName}", color = if (available != null) NavGold else NavMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        if (available != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isDeferred) {
                        if (isEn) "Postponed" else "Pospuesta"
                    } else {
                        available.releaseName
                    },
                    modifier = Modifier.weight(1f), color = Color(0xFFD8DEE7), fontSize = 10.sp, maxLines = 1
                )
                TextButton(onClick = { onUpdate(available) }, enabled = available.apkUrl != null) {
                    Text(if (isEn) "Update" else "Actualizar", color = NavGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                TextButton(onClick = { onDefer(available) }) {
                    Text(if (isEn) "Not now" else "Ahora no", color = NavMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

private data class NavEntry(val screen: Screen, val labelEs: String, val labelEn: String, val icon: ImageVector)

@Composable
private fun BottomNav(current: Screen, isEn: Boolean, gameEnabled: Boolean, onSelect: (Screen) -> Unit) {
    val entries = listOf(
        NavEntry(Screen.HOME, "Inicio", "Home", Icons.Filled.Home),
        NavEntry(Screen.GAME, "Partida", "Game", Icons.Filled.PlayArrow),
        NavEntry(Screen.PROFILE, "Perfil", "Profile", Icons.Filled.Person),
        NavEntry(Screen.SETTINGS, "Ajustes", "Settings", Icons.Filled.Settings)
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(NavLine))
        Row(
            modifier = Modifier.fillMaxWidth().background(NavBg).padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            entries.forEach { entry ->
                val active = current == entry.screen
                val enabled = entry.screen != Screen.GAME || gameEnabled
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0xFF1C232D) else Color.Transparent)
                        .clickable(enabled = enabled) { onSelect(entry.screen) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val tint = when {
                        active -> NavGold
                        enabled -> NavMuted
                        else -> NavMuted.copy(alpha = 0.35f)
                    }
                    Icon(entry.icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                    Text(if (isEn) entry.labelEn else entry.labelEs, color = tint, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}