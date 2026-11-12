package com.chessx.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal data class StockfishStrength(val uciElo: Int, val skillLevel: Int)

sealed interface EngineInstallState {
    object Idle : EngineInstallState
    object Ready : EngineInstallState
    data class Error(val message: String) : EngineInstallState
}

class StockfishEngineManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val engineDirectory = File(appContext.filesDir, "chess-engine")
    private val engineFile = File(appContext.applicationInfo.nativeLibraryDir, "libstockfish.so")
    private val engineMutex = Mutex()
    private val _installState = MutableStateFlow(
        if (isInstalled()) EngineInstallState.Ready
        else EngineInstallState.Error(unavailableReason())
    )
    val installState: StateFlow<EngineInstallState> = _installState.asStateFlow()

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var output: Channel<String>? = null
    private var outputJob: Job? = null

    fun isInstalled(): Boolean =
        "arm64-v8a" in Build.SUPPORTED_ABIS && engineFile.isFile && engineFile.canExecute()

    private fun unavailableReason(): String =
        if ("arm64-v8a" in Build.SUPPORTED_ABIS) {
            "Este APK no incluye el motor Stockfish"
        } else {
            "Stockfish requiere un dispositivo Android ARM64"
        }

    suspend fun bestMove(
        moves: List<String>,
        strengthPercent: Int
    ): String = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            ensureProcess()
            val strength = strengthForPercent(strengthPercent)
            send("setoption name UCI_LimitStrength value true")
            send("setoption name UCI_Elo value ${strength.uciElo}")
            send("setoption name Skill Level value ${strength.skillLevel}")
            send("isready")
            awaitLine { it == "readyok" }
            send("ucinewgame")
            send(if (moves.isEmpty()) "position startpos" else "position startpos moves ${moves.joinToString(" ")}")
            send("go movetime $MOVE_TIME_MS")

            var bestMove = ""
            while (true) {
                val line = receiveLine()
                if (line.startsWith("bestmove ")) {
                    bestMove = line.substringAfter("bestmove ").substringBefore(' ').trim()
                    break
                }
            }
            if (bestMove.isBlank() || bestMove == "(none)" || bestMove == "0000") return@withLock ""
            bestMove
        }
    }

    fun stop() {
        val running = process
        if (running?.isAlive == true) running.destroy()
        outputJob?.cancel()
        output?.close()
        process = null
        writer = null
        output = null
        outputJob = null
    }

    private suspend fun ensureProcess() {
        if (process?.isAlive == true) return
        if (!isInstalled()) throw IllegalStateException(unavailableReason())
        engineDirectory.mkdirs()
        val started = ProcessBuilder(engineFile.absolutePath)
            .directory(engineDirectory)
            .redirectErrorStream(true)
            .start()
        val channel = Channel<String>(Channel.UNLIMITED)
        process = started
        writer = BufferedWriter(OutputStreamWriter(started.outputStream, StandardCharsets.UTF_8))
        output = channel
        outputJob = scope.launch {
            started.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> channel.trySend(line) }
            }
            channel.close()
        }
        send("uci")
        awaitLine { it == "uciok" }
        send("setoption name Threads value 1")
        send("setoption name Hash value 16")
        send("isready")
        awaitLine { it == "readyok" }
    }

    private fun send(command: String) {
        val currentWriter = writer ?: throw IllegalStateException("El motor no está listo")
        currentWriter.write(command)
        currentWriter.newLine()
        currentWriter.flush()
    }

    private suspend fun awaitLine(predicate: (String) -> Boolean) {
        while (true) {
            if (predicate(receiveLine())) return
        }
    }

    private suspend fun receiveLine(): String = withTimeout(UCI_TIMEOUT_MS) {
        output?.receive() ?: throw IllegalStateException("El motor se cerró inesperadamente")
    }

    companion object {
        private const val UCI_MIN_ELO = 1320
        private const val UCI_MAX_ELO = 3190
        private const val MIN_STRENGTH_PERCENT = 0
        private const val MAX_STRENGTH_PERCENT = 100
        private const val MAX_SKILL_LEVEL = 20
        private const val MOVE_TIME_MS = 900
        private const val UCI_TIMEOUT_MS = 30_000L

        @Volatile
        private var instance: StockfishEngineManager? = null

        fun getInstance(context: Context): StockfishEngineManager =
            instance ?: synchronized(this) {
                instance ?: StockfishEngineManager(context).also { instance = it }
            }

        internal fun strengthForPercent(percent: Int): StockfishStrength {
            val normalized = percent.coerceIn(MIN_STRENGTH_PERCENT, MAX_STRENGTH_PERCENT)
            val eloRange = UCI_MAX_ELO - UCI_MIN_ELO
            return StockfishStrength(
                uciElo = UCI_MIN_ELO + (eloRange * normalized / MAX_STRENGTH_PERCENT),
                skillLevel = MAX_SKILL_LEVEL * normalized / MAX_STRENGTH_PERCENT
            )
        }
    }
}
