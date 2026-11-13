package com.bchess.android.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.bchess.android.R

enum class SoundType {
    MOVE, CAPTURE, CHECK, CHECKMATE, ILLEGAL, CLICK, WIN, LOSE, DRAW, PAUSE, LOWTIME, COUNTDOWN3, COUNTDOWN2, COUNTDOWN1
}

/**
 * Motor de sonidos real basado en los MP3 oficiales de Lichess.
 * Set "standard" (movimiento, captura, selección, error, confirmación, cuenta
 * atrás, tiempo bajo) completado con los avisos de fin de partida del set
 * "sfx" — sin sonidos de naturaleza ni de animales.
 * Cargados en un SoundPool: latencia baja, seguro en cualquier API 26+.
 */
class SoundManager(private val context: Context, pack: SoundPack = SoundPack.DEFAULT) {
    private var enabled: Boolean = true
    private var volumePercent: Int = 80
    private var soundPack: SoundPack = pack

    private var pool: SoundPool? = try {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
    } catch (e: Exception) {
        Log.w("Sound", "SoundPool unavailable", e)
        null
    }

    private var moveId = 0
    private var captureId = 0
    private var checkId = 0
    private var checkmateId = 0
    private var illegalId = 0
    private var selectId = 0
    private var winId = 0
    private var loseId = 0
    private var drawId = 0
    private var pauseId = 0
    private var confirmId = 0
    private var lowTimeId = 0
    private var countdown3Id = 0
    private var countdown2Id = 0
    private var countdown1Id = 0

    init {
        moveId = loadSound(R.raw.move)
        captureId = loadSound(R.raw.capture)
        checkId = loadSound(R.raw.check)
        checkmateId = loadSound(R.raw.checkmate)
        illegalId = loadSound(R.raw.error)
        selectId = loadSound(R.raw.select)
        winId = loadSound(R.raw.victory)
        loseId = loadSound(R.raw.defeat)
        drawId = loadSound(R.raw.draw)
        confirmId = loadSound(R.raw.confirmation)
        lowTimeId = loadSound(R.raw.lowtime)
        countdown3Id = loadSound(R.raw.countdown3)
        countdown2Id = loadSound(R.raw.countdown2)
        countdown1Id = loadSound(R.raw.countdown1)
        pauseId = moveId // pausa: sonido suave, no alarma
    }

    private fun loadSound(resourceId: Int): Int {
        return try {
            pool?.load(context, resourceId, 1) ?: 0
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to load sound $resourceId", e)
            0
        }
    }

    fun setEnabled(enabled: Boolean) { this.enabled = enabled }
    fun isEnabled(): Boolean = enabled
    fun setVolume(percent: Int) { volumePercent = percent.coerceIn(0, 100) }
    fun getVolume(): Int = volumePercent
    fun setPack(pack: SoundPack) { soundPack = pack }

    /**
     * Vista previa de un paquete de sonidos para la pantalla de Ajustes:
     * reproduce un movimiento con el paquete indicado sin alterar la
     * configuración actual. Suena aunque los efectos estén desactivados
     * (es sólo una demostración sonora).
     */
    fun playPreview(pack: SoundPack) {
        val previous = soundPack
        soundPack = pack
        val id = moveId
        if (id > 0 && pool != null) {
            try { pool?.play(id, vol(), vol(), 1, 0, pitch()) } catch (_: Exception) { }
        }
        soundPack = previous
    }

    private fun vol(): Float = (volumePercent / 100f).coerceIn(0f, 1f)
    private fun pitch(): Float = when (soundPack) {
        SoundPack.LICHESS -> 1f
        SoundPack.FREE -> 1.08f
        SoundPack.CLASSIC -> 0.94f
    }

    fun play(type: SoundType) {
        if (!enabled) return

        val id = when (type) {
            SoundType.MOVE -> moveId
            SoundType.CAPTURE -> captureId
            SoundType.CHECK -> checkId
            SoundType.CHECKMATE -> checkmateId
            SoundType.ILLEGAL -> illegalId
            SoundType.CLICK -> selectId
            SoundType.WIN -> winId
            SoundType.LOSE -> loseId
            SoundType.DRAW -> drawId
            SoundType.PAUSE -> pauseId
            SoundType.LOWTIME -> lowTimeId
            SoundType.COUNTDOWN3 -> countdown3Id
            SoundType.COUNTDOWN2 -> countdown2Id
            SoundType.COUNTDOWN1 -> countdown1Id
        }

        if (id <= 0 || pool == null) {
            // El motor de audio no está disponible o el sonido no cargó: no-op silencioso.
            return
        }

        try {
            pool?.play(id, vol(), vol(), 1, 0, pitch())
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to play $type", e)
        }
    }

    /** Sonido de confirmación (aceptar conexiones / iniciar partida). */
    /** Sonido de confirmación (aceptar conexiones / iniciar partida). */
    fun playConfirmation() {
        if (!enabled) return
        if (confirmId > 0) {
            try {
                pool?.play(confirmId, vol(), vol(), 1, 0, pitch())
                Log.d("SoundManager", "Confirmation sound played")
            } catch (e: Exception) {
                Log.e("SoundManager", "Failed to play confirmation", e)
            }
        } else {
            Log.w("SoundManager", "Confirmation sound ID is invalid: $confirmId")
        }
    }

    fun release() {
        try { pool?.release() } catch (_: Exception) {}
    }
}
