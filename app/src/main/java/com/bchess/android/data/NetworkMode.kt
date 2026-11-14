package com.bchess.android.data

/** Transporte de red activo para la partida de dos jugadores. */
enum class NetworkMode {
    /** Bluetooth clásico (actual, con heartbeat + reconexión automática). */
    BT,
    /** Internet P2P (WebRTC + señalización Firebase). Código de partida. */
    INTERNET,
    /** LAN/red local o misma PC: TCP directo por IP:puerto (cross-play con PC). */
    TCP
}
