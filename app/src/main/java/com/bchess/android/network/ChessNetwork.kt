/*
 * B-Chess · © Elal Chico
 * Capa de abstracción de red: Bluetooth (actual) e Internet P2P (WebRTC/Firebase).
 * El motor y la UI no necesitan saber qué transporte se usa.
 */
package com.bchess.android.network

interface ChessNetwork {
    interface Listener {
        fun onConnected()
        /** Mensaje de ajedrez en formato JSON line (mismo protocolo que Bluetooth). */
        fun onMessageLine(line: String)
        fun onDisconnected(reason: String)
        fun onError(error: String)
    }

    fun setListener(listener: Listener)

    /** Rol de Anfitrión: crea la partida y genera la oferta WebRTC/BT. */
    fun createMatch(onMatchIdGenerated: (String) -> Unit)

    /** Rol de Invitado: se une a una partida existente por código. */
    fun joinMatch(matchId: String)

    /** Envía un mensaje del protocolo de juego (JSON line). */
    fun sendLine(line: String)

    /** Limpieza de recursos. */
    fun disconnect()
}
