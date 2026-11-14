/*
 * B-Chess · © Elal Chico
 * Modo LAN / cross-play PC↔móvil: conexión TCP directa por IP:puerto usando el
 * protocolo de líneas de la versión de PC (DirectPeer). Es un adaptador:
 *
 *   móvil (JSON BtMessage)  ⇄  protocolo de líneas del PC  ⇄  PC
 *
 * Protocolo PC (texto, una línea por mensaje, terminado en \n):
 *   BCHESS-PC 1                 — apretón de manos (ambos lo envían al conectar)
 *   START <timeMs> <incMs> <w|b> — el anfitrión inicia la partida (color del anfitrión)
 *   REMATCH <same|random>        — el invitado pide revancha
 *   MOVE <uci>                   — jugada (p.ej. MOVE e7e5)
 *   PAUSE <0|1>                  — pausa / reanudar
 *   DRAW / DRAW_ACCEPT / DRAW_DECLINE — oferta de tablas
 *   RESIGN                       — rendición
 *   END <w|b|-> <RAZON>          — fin (p.ej. por tiempo)
 *   PING / PONG                  — latido
 *
 * Decisiones de diseño:
 * - El anfitrión (quien abre la sala) es autoridad de la partida: decide colores y
 *   cadencia y lo anuncia con START. El invitado nunca fuerza un reinicio si la
 *   partida ya está en marcha con jugadas.
 * - El reloj del móvil corre en local en ambos roles (el PC tampoco sincroniza
 *   relojes), así que ambos dispositivos miden el tiempo de forma independiente.
 * - Los mensajes que no existen en el PC (perfil, lobby, reloj, torneo) se
 *   ignoran/ eliminan silenciosamente para no romper al PC.
 */
package com.bchess.android.network

import android.util.Log
import com.bchess.android.core.Move
import com.bchess.android.data.BtMessage
import com.bchess.android.data.BtState
import com.bchess.android.data.ClockSyncMessage
import com.bchess.android.data.DisconnectMessage
import com.bchess.android.data.DrawOfferMessage
import com.bchess.android.data.DrawResponseMessage
import com.bchess.android.data.GameEndMessage
import com.bchess.android.data.HelloMessage
import com.bchess.android.data.LobbyConfigMessage
import com.bchess.android.data.MoveMessage
import com.bchess.android.data.PauseMessage
import com.bchess.android.data.PingMessage
import com.bchess.android.data.PongMessage
import com.bchess.android.data.RematchMessage
import com.bchess.android.data.ResignMessage
import com.bchess.android.data.StartGameMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "TcpBridge"
private const val HANDSHAKE = "BCHESS-PC 1"
private const val STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

class TcpBridgeNetwork(private val scope: CoroutineScope) : ChessNetwork {

    companion object {
        const val DEFAULT_PORT = 5891

        /** IPs IPv4 locales (no loopback) para compartir como invitación. */
        fun localIpv4(): List<String> {
            val out = mutableListOf<String>()
            try {
                for (nif in NetworkInterface.getNetworkInterfaces()) {
                    if (!nif.isUp || nif.isLoopback) continue
                    for (addr in nif.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            out.add(addr.hostAddress ?: continue)
                        }
                    }
                }
            } catch (_: Exception) {}
            return out
        }
    }

    // ===== API pública (mismo estilo que WebRTCNetworkManager) =====
    private val _state = MutableStateFlow(BtState.IDLE)
    val state: StateFlow<BtState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Puerto real abierto (host) o "ip:puerto" del rival (invitado). */
    private val _currentMatchId = MutableStateFlow<String?>(null)
    val currentMatchId: StateFlow<String?> = _currentMatchId.asStateFlow()

    private val _incoming = MutableSharedFlow<BtMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<BtMessage> = _incoming.asSharedFlow()

    /** Color del lado PC ("white" | "black"). Necesario para traducir RESIGN. */
    var localColorProvider: () -> String = { "white" }
        set(value) { field = value }

    /** true = yo abrí la sala (anfitrión, blanco por defecto). */
    var isHost: Boolean = false
        private set

    @Volatile private var pcColor: String = "white"
    private var boundPort: Int = -1

    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: Thread? = null
    private var acceptThread: Thread? = null
    private var listener: ChessNetwork.Listener? = null

    // ===== ChessNetwork =====
    override fun setListener(l: ChessNetwork.Listener) {
        listener = l
    }

    /** Abre la sala como anfitrión. port 0 = puerto libre aleatorio. */
    fun createMatch(port: Int, onMatchIdGenerated: (String) -> Unit = {}) {
        closeQuietly()
        isHost = true
        _state.value = BtState.WAITING_HOST
        _lastError.value = null

        try {
            val ss = ServerSocket()
            ss.bind(InetSocketAddress(if (port > 0) port else 0))
            serverSocket = ss
            boundPort = ss.localPort
            _currentMatchId.value = boundPort.toString()
            onMatchIdGenerated(boundPort.toString())
        } catch (e: Exception) {
            fail("No se pudo abrir el puerto $port: ${e.message}")
            return
        }

        running.set(true)
        acceptThread = Thread {
            try {
                val s = serverSocket?.accept() ?: return@Thread
                adopt(s)
            } catch (e: Exception) {
                if (running.get()) fail("Error en la sala: ${e.message}")
            }
        }.apply { isDaemon = true; name = "bchess-tcp-host" }
        acceptThread?.start()
    }

    override fun createMatch(onMatchIdGenerated: (String) -> Unit) {
        createMatch(DEFAULT_PORT, onMatchIdGenerated)
    }

    /** Se une a la sala de un PC. Acepta "ip" (puerto 5891) o "ip:puerto". */
    override fun joinMatch(matchId: String) {
        closeQuietly()
        isHost = false

        val clean = matchId.trim()
        val host: String
        val port: Int
        if (':' in clean) {
            val parts = clean.split(':')
            host = parts[0].trim()
            port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: -1
        } else {
            host = clean
            port = DEFAULT_PORT
        }
        if (host.isBlank() || port <= 0 || port > 65535) {
            fail("Dirección inválida. Usa IP o IP:puerto (p.ej. 192.168.1.10:$DEFAULT_PORT)")
            return
        }

        _state.value = BtState.CONNECTING
        _lastError.value = null
        _currentMatchId.value = "$host:$port"
        running.set(true)

        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 10_000)
                adopt(s)
            } catch (e: Exception) {
                if (running.get()) {
                    running.set(false)
                    fail("No se pudo conectar con $host:$port — ${e.message}")
                }
            }
        }
    }

    override fun sendLine(line: String) {
        if (line.isBlank()) return
        val w = writer
        if (w == null) {
            Log.w(TAG, "sendLine sin conexión: $line")
            return
        }
        try {
            synchronized(w) {
                w.write(line)
                w.newLine()
                w.flush()
            }
        } catch (e: Exception) {
            fail("Error enviando: ${e.message}")
        }
    }

    fun sendMessage(message: BtMessage) {
        val line = toLine(message) ?: return
        sendLine(line)
    }

    override fun disconnect() {
        closeQuietly()
        if (_state.value != BtState.IDLE) _state.value = BtState.DISCONNECTED
    }

    // ===== Conexión y lectura =====
    private fun adopt(s: Socket) {
        try {
            socket = s
            s.tcpNoDelay = true
            val r = BufferedReader(InputStreamReader(s.getInputStream()))
            val w = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
            writer = w
            sendLine(HANDSHAKE)
            _state.value = BtState.CONNECTED
            listener?.onConnected()

            reader = Thread {
                try {
                    while (running.get() && !s.isClosed) {
                        val line = r.readLine() ?: break
                        if (line.isNotBlank()) handleLine(line)
                    }
                } catch (_: Exception) {
                    // caída del rival: se trata abajo
                } finally {
                    if (running.get()) {
                        running.set(false)
                        _state.value = BtState.DISCONNECTED
                        try { listener?.onDisconnected("La conexión se cerró") } catch (_: Exception) {}
                    }
                }
            }.apply { isDaemon = true; name = "bchess-tcp-reader" }
            reader?.start()
        } catch (e: Exception) {
            fail("Error de conexión: ${e.message}")
        }
    }

    private fun closeQuietly() {
        running.set(false)
        try { socket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        socket = null
        serverSocket = null
        writer = null
    }

    private fun fail(message: String) {
        _state.value = BtState.ERROR
        _lastError.value = message
        Log.e(TAG, message)
        try { listener?.onError(message) } catch (_: Exception) {}
    }

    // ===== PC → móvil (línea → BtMessage) =====
    private fun handleLine(raw: String) {
        val line = raw.trim()
        when {
            line.startsWith("BCHESS-PC") -> {
                _state.value = BtState.CONNECTED
                try { listener?.onConnected() } catch (_: Exception) {}
                // Fija el nombre del rival para la UI
                emit(HelloMessage("PC", isHost, pcColor))
            }
            line.startsWith("START ") -> {
                val p = line.split(' ')
                val timeMs = p.getOrNull(1)?.toLongOrNull() ?: return
                val incMs = p.getOrNull(2)?.toLongOrNull() ?: 0L
                val hostColor = if (p.getOrNull(3) == "b") "black" else "white"
                pcColor = hostColor
                emit(
                    StartGameMessage(
                        startFen = STANDARD_FEN,
                        timeMs = timeMs,
                        incrementMs = incMs,
                        hostColor = hostColor,
                        hostName = "PC",
                        guestName = "",
                        hostTimestamp = System.currentTimeMillis()
                    )
                )
            }
            line.startsWith("MOVE ") -> {
                val uci = line.substring(5).trim()
                if (uci.length < 4) return
                val from = Move.algebraicToSquare(uci.substring(0, 2)) ?: return
                val to = Move.algebraicToSquare(uci.substring(2, 4)) ?: return
                val promotion = when (uci.getOrNull(4)) {
                    'q' -> "Q"; 'r' -> "R"; 'b' -> "B"; 'n' -> "N"; else -> ""
                }
                emit(
                    MoveMessage(
                        from = from, to = to, promotion = promotion,
                        san = "", uci = uci, fenAfter = "",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            line == "RESIGN" ->
                emit(ResignMessage("PC", pcColor))
            line == "DRAW" ->
                emit(DrawOfferMessage("PC"))
            line == "DRAW_ACCEPT" ->
                emit(DrawResponseMessage(true, "PC"))
            line == "DRAW_DECLINE" ->
                emit(DrawResponseMessage(false, "PC"))
            line.startsWith("REMATCH") -> {
                val mode = line.split(' ').getOrNull(1) ?: "same"
                emit(RematchMessage(mode, "PC"))
            }
            line.startsWith("END ") -> {
                val p = line.split(' ')
                val winner = when (p.getOrNull(1)) {
                    "w" -> "white"; "b" -> "black"; else -> ""
                }
                val reason = p.getOrNull(2) ?: "TIMEOUT"
                emit(GameEndMessage(winner, reason, "Partida terminada"))
            }
            line.startsWith("PAUSE") -> {
                val p = line.split(' ').getOrNull(1)
                when (p) {
                    "1" -> emit(PauseMessage(true, "PC"))
                    "0" -> emit(PauseMessage(false, "PC"))
                }
            }
            line == "PING" -> sendLine("PONG")
            line == "DISCONNECT" -> {
                val msg = DisconnectMessage("PC", "user_disconnect")
                emit(msg)
                // El rival cortó: limpiar conexión
                closeQuietly()
                _state.value = BtState.DISCONNECTED
            }
            else -> Log.d(TAG, "Línea ignorada del PC: $line")
        }
    }

    private fun emit(message: BtMessage) {
        if (!_incoming.tryEmit(message)) {
            scope.launch { _incoming.emit(message) }
        }
        try { listener?.onMessageLine(message.toJsonLine()) } catch (_: Exception) {}
    }

    // ===== móvil → PC (BtMessage → línea) =====
    private fun toLine(message: BtMessage): String? = when (message) {
        is HelloMessage -> HANDSHAKE
        is StartGameMessage -> {
            val letter = if (message.hostColor == "black") "b" else "w"
            pcColor = if (letter == "w") "black" else "white"
            "START ${message.timeMs} ${message.incrementMs} $letter h"
        }
        is MoveMessage -> "MOVE ${message.uci}"
        is DrawOfferMessage -> "DRAW"
        is DrawResponseMessage -> if (message.accepted) "DRAW_ACCEPT" else "DRAW_DECLINE"
        is ResignMessage -> "RESIGN"
        is RematchMessage -> "REMATCH ${message.mode}"
        is PauseMessage -> "PAUSE ${if (message.paused) 1 else 0}"
        is GameEndMessage -> {
            val letter = when (message.winnerColor) {
                "white" -> "w"; "black" -> "b"; else -> "-"
            }
            "END $letter ${message.reason}"
        }
        is DisconnectMessage -> "DISCONNECT"
        is ClockSyncMessage -> null // el PC no sincroniza relojes
        is LobbyConfigMessage -> null
        is PingMessage -> "PING"
        is PongMessage -> "PONG"
        else -> null // perfil, torneo, etc.: no existen en el PC
    }
}
