/*
 * B-Chess · © Elal Chico
 * Modo Internet P2P: señalización Firebase (solo handshake) + WebRTC DataChannel
 * (SCTP confiable y ordenado). Es el hermano intercambiable de BluetoothClassicManager
 * y transporta EXACTAMENTE el mismo protocolo de juego (BtMessage JSON line),
 * por lo que el motor y la UI funcionan igual en ambos modos.
 */
package com.bchess.android.network

import android.content.Context
import android.util.Log
import com.bchess.android.data.BtMessage
import com.bchess.android.data.BtState
import com.bchess.android.data.MessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.*
import org.json.JSONObject

private const val TAG = "WebRTC"

/**
 * WebRTC Network Manager — el juego se juega idéntico en Bluetooth e Internet.
 *
 * Flujo anfitrión: createMatch() genera matchId -> invitado lo ingresa ->
 * PeerConnection + DataChannel (ordered, reliable) -> listener.onConnected()
 * Flujo invitado:   joinMatch(matchId) -> escucha "offer" en Firebase ->
 * setRemoteDescription -> answer -> DataChannel.
 *
 * NOTA: sin servidor TURN aún; el 85-90% de las redes negocian directo por
 * STUN público de Google. Para el 15% restante (NAT simétrico estricto),
 * añade un servidor TURN en iceServers o aparecerá como error de conexión.
 */
class WebRTCNetworkManager(
    private val context: Context,
    private val scope: CoroutineScope
) : ChessNetwork {

    companion object {
        val STUN_GOOGLE =
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    }

    private var listener: ChessNetwork.Listener? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var matchId: String = ""
    private var role: String = "" // "host" o "guest"
    private var watchdog: Job? = null

    // Estado público equivalente a BluetoothClassicManager (UI y GameViewModel iguales)
    private val _state = MutableStateFlow(BtState.IDLE)
    val state: StateFlow<BtState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _matchId = MutableStateFlow<String?>(null)
    val currentMatchId: StateFlow<String?> = _matchId.asStateFlow()

    private val _incoming = MutableSharedFlow<BtMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<BtMessage> = _incoming.asSharedFlow()

    /** ¿El usuario es anfitrión de esta partida? */
    @Volatile
    var isHost: Boolean = false
        private set

    @Volatile
    var hostName: String = "Rival"

    override fun setListener(listener: ChessNetwork.Listener) {
        this.listener = listener
    }

    override fun createMatch(onMatchIdGenerated: (String) -> Unit) {
        createMatch(null, onMatchIdGenerated)
    }

    /**
     * @param customCode código elegido por el anfitrión (null/vacío = automático).
     *   Firebase solo verifica en ese momento si el código está libre; no guarda
     *   una lista persistente y el nodo se borra al terminar la conexión.
     */
    fun createMatch(customCode: String?, onMatchIdGenerated: (String) -> Unit) {
        role = "host"
        isHost = true
        disconnectPeer() // reset defensivo por si había una sesión previa

        val signaling = signalingOrNull()
        if (signaling == null) {
            fail(SignalingManager.MISSING_ERROR)
            return
        }

        _state.value = BtState.WAITING_HOST
        _lastError.value = null

        signaling.resolveMatchId(customCode, onResult = { id ->
            matchId = id
            _matchId.value = id
            onMatchIdGenerated(id)
            signaling.touchMatch(id)
            openPeerConnection(signaling)

            // Host crea el DataChannel y la oferta SDP
            val dcInit = DataChannel.Init().apply {
                ordered = true // los movimientos llegan en orden, incluso con micro-cortes
            }
            val local = peerConnection?.createDataChannel("chess-data", dcInit)
            setupDataChannel(local)

            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    val sdp = sessionDescription?.description ?: return
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() { signaling.saveOffer(matchId, sdp, {}) }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, sessionDescription)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) = fail("No se pudo crear la oferta")
                override fun onSetFailure(p0: String?) {}
            }, MediaConstraints())

            // Host espera la respuesta del invitado
            signaling.listenForAnswer(matchId) { answerSdp ->
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
            }
            signaling.listenForIceCandidates(matchId, "guest") { candidateJson ->
                addRemoteIceCandidate(candidateJson)
            }

            startWatchdog() // sin conexión en 90 s -> error legible
        }, onError = { message ->
            fail(message)
        })
    }

    override fun joinMatch(matchId: String) {
        role = "guest"
        isHost = false
        disconnectPeer()
        val signaling0 = signalingOrNull()
        this.matchId = signaling0?.normalizeJoinCode(matchId) ?: matchId.trim()
        if (this.matchId.isBlank()) {
            fail("Código de partida inválido")
            return
        }

        val signaling = signaling0
        if (signaling == null) {
            fail(SignalingManager.MISSING_ERROR)
            return
        }

        _state.value = BtState.CONNECTING
        _lastError.value = null
        openPeerConnection(signaling)

        val currentId = this.matchId
        // Invitado escucha la oferta del anfitrión
        signaling.listenForOffer(currentId) { offerSdp ->
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    peerConnection?.createAnswer(object : SdpObserver {
                        override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                            val sdp = sessionDescription?.description ?: return
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    signaling.saveAnswer(currentId, sdp, {})
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            }, sessionDescription)
                        }
                        override fun onSetSuccess() {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, MediaConstraints())
                }
                override fun onCreateFailure(p0: String?) = fail("No se pudo aplicar la oferta")
                override fun onSetFailure(p0: String?) = fail("No se pudo aplicar la oferta remota")
            }, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        }
        signaling.listenForIceCandidates(currentId, "host") { candidateJson ->
            addRemoteIceCandidate(candidateJson)
        }

        startWatchdog()
    }

    /** Envía una línea de protocolo (BtMessage JSON line) por el DataChannel. */
    fun sendMessage(message: BtMessage) {
        sendLine(message.toJsonLine())
    }

    override fun sendLine(line: String) {
        val dc = dataChannel ?: run {
            Log.w(TAG, "sendLine ignored: data channel not open")
            return
        }
        try {
            if (dc.state() != DataChannel.State.OPEN) {
                Log.w(TAG, "sendLine ignored: channel state=${dc.state()}")
                return
            }
            dc.send(DataChannel.Buffer(java.nio.ByteBuffer.wrap(line.toByteArray(Charsets.UTF_8)), false))
        } catch (e: Exception) {
            Log.e(TAG, "sendLine failed", e)
            _lastError.value = "No se pudo enviar el movimiento"
        }
    }

    override fun disconnect() {
        watchdog?.cancel()
        disconnectPeer()
        val id = matchId
        matchId = ""
        _matchId.value = null
        try {
            FirebaseBootstrap.get().let { bootstrap ->
                if (bootstrap is FirebaseInit.Ok && id.isNotBlank()) {
                    SignalingManager(bootstrap.database).cleanupMatch(id)
                }
            }
        } catch (_: Exception) {}
        _state.value = BtState.IDLE
        listener?.onDisconnected("Desconectado")
    }

    @Synchronized
    private fun openPeerConnection(signaling: SignalingManager) {
        ensureFactory()
        val rtcConfig = PeerConnection.RTCConfiguration(listOf(STUN_GOOGLE))
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                iceCandidate ?: return
                val json = JSONObject().apply {
                    put("sdpMid", iceCandidate.sdpMid)
                    put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                    put("candidate", iceCandidate.sdp)
                }
                signaling.saveIceCandidate(matchId, role, json.toString())
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        watchdog?.cancel()
                        _state.value = BtState.CONNECTED
                        _lastError.value = null
                        listener?.onConnected()
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.CLOSED -> {
                        if (_state.value != BtState.IDLE) {
                            _state.value = BtState.DISCONNECTED
                            listener?.onDisconnected("Conexión perdida")
                        }
                    }
                    else -> Unit
                }
            }

            override fun onDataChannel(channel: DataChannel?) {
                // Solo el invitado recibe la apertura del canal del anfitrión
                if (role == "guest") setupDataChannel(channel)
            }

            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onIceCandidateError(p0: IceCandidateErrorEvent?) {}
        }
        peerConnection = factory?.createPeerConnection(rtcConfig, observer)
        if (peerConnection == null) {
            fail("No se pudo crear la conexión P2P")
        }
    }

    private fun setupDataChannel(dc: DataChannel?) {
        dataChannel = dc
        dc?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {}

            override fun onStateChange() {
                if (dc.state() == DataChannel.State.OPEN) {
                    _state.value = BtState.CONNECTED
                    _lastError.value = null
                    listener?.onConnected()
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer ?: return
                try {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    val line = String(bytes, Charsets.UTF_8).trim()
                    if (line.isEmpty()) return
                    MessageParser.parse(line)?.let { msg ->
                        scope.launch(Dispatchers.Default) { _incoming.emit(msg) }
                        listener?.onMessageLine(line)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onMessage failed", e)
                }
            }
        })
    }

    private fun addRemoteIceCandidate(candidateJson: String) {
        try {
            val json = JSONObject(candidateJson)
            peerConnection?.addIceCandidate(
                IceCandidate(
                    json.getString("sdpMid"),
                    json.getInt("sdpMLineIndex"),
                    json.getString("candidate")
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "bad ice candidate: $candidateJson", e)
        }
    }

    private fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
    }

    private fun disconnectPeer() {
        watchdog?.cancel()
        try {
            dataChannel?.close()
            dataChannel?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (_: Exception) {}
        dataChannel = null
        peerConnection = null
    }

    private fun signalingOrNull(): SignalingManager? {
        val bootstrap = FirebaseBootstrap.get()
        return when (bootstrap) {
            is FirebaseInit.Ok -> SignalingManager(bootstrap.database)
            is FirebaseInit.Missing -> {
                Log.e(TAG, "Firebase not configured: ${bootstrap.error}")
                null
            }
        }
    }

    private fun fail(message: String) {
        _state.value = BtState.ERROR
        _lastError.value = message
        listener?.onError(message)
    }

    private fun startWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(90_000)
            if (_state.value != BtState.CONNECTED) {
                fail("No se pudo conectar en 90 s. Revisa tu red o el código de partida.")
            }
        }
    }
}
