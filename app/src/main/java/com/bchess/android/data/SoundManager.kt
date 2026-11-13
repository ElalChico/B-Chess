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
 * Motor de sonidos.
 *
 * Paquetes (cada uno con archivos reales distintos, no sólo cambio de pitch):
 *  - LICHESS: pack estándar de Lichess (los MP3 oficiales incluidos en la app).
 *  - GM3000:  librería de efectos GM-3000 (carpeta public/sounds del proyecto GM-3000).
 *  - CLASSIC: los mismos efectos GM-3000 pero con tono profundo/retro arcade
 *             (pitch 0.75) para quien prefiera un carácter clásico.
 *
 * El sonido de selección de pieza (SoundType.CLICK) está desactivado en todos
 * los paquetes a petición del usuario: seleccionar una pieza ya no hace ruido.
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

    // Mapa paquete -> tipo de sonido -> id cargado en el SoundPool
    private val packedIds: Map<SoundPack, Map<SoundType, Int>>
    // Sonido de confirmación/inicio por paquete
    private val confirmIds: Map<SoundPack, Int>

    init {
        packedIds = mapOf(
            SoundPack.LICHESS to mapOf(
                SoundType.MOVE to loadSound(R.raw.move),
                SoundType.CAPTURE to loadSound(R.raw.capture),
                SoundType.CHECK to loadSound(R.raw.check),
                SoundType.CHECKMATE to loadSound(R.raw.checkmate),
                SoundType.ILLEGAL to loadSound(R.raw.error),
                SoundType.WIN to loadSound(R.raw.victory),
                SoundType.LOSE to loadSound(R.raw.defeat),
                SoundType.DRAW to loadSound(R.raw.draw),
                SoundType.PAUSE to loadSound(R.raw.move),
                SoundType.LOWTIME to loadSound(R.raw.lowtime),
                SoundType.COUNTDOWN3 to loadSound(R.raw.countdown3),
                SoundType.COUNTDOWN2 to loadSound(R.raw.countdown2),
                SoundType.COUNTDOWN1 to loadSound(R.raw.countdown1)
            ),
            SoundPack.GM3000 to mapOf(
                SoundType.MOVE to loadSound(R.raw.gm_move),
                SoundType.CAPTURE to loadSound(R.raw.gm_capture),
                SoundType.CHECK to loadSound(R.raw.gm_jaque),
                SoundType.CHECKMATE to loadSound(R.raw.gm_mate),
                SoundType.ILLEGAL to loadSound(R.raw.gm_error),
                SoundType.WIN to loadSound(R.raw.gm_victory),
                SoundType.LOSE to loadSound(R.raw.gm_tiempofinalizado),
                SoundType.DRAW to loadSound(R.raw.gm_draw),
                SoundType.PAUSE to loadSound(R.raw.gm_save),
                SoundType.LOWTIME to loadSound(R.raw.gm_torrefast),
                SoundType.COUNTDOWN3 to loadSound(R.raw.gm_torrefast),
                SoundType.COUNTDOWN2 to loadSound(R.raw.gm_torrefast),
                SoundType.COUNTDOWN1 to loadSound(R.raw.gm_torrefast)
            ),
            SoundPack.CLASSIC to mapOf(
                SoundType.MOVE to loadSound(R.raw.gm_move),
                SoundType.CAPTURE to loadSound(R.raw.gm_capture),
                SoundType.CHECK to loadSound(R.raw.gm_jaque),
                SoundType.CHECKMATE to loadSound(R.raw.gm_mate),
                SoundType.ILLEGAL to loadSound(R.raw.gm_error),
                SoundType.WIN to loadSound(R.raw.gm_victory),
                SoundType.LOSE to loadSound(R.raw.gm_tiempofinalizado),
                SoundType.DRAW to loadSound(R.raw.gm_draw),
                SoundType.PAUSE to loadSound(R.raw.gm_save),
                SoundType.LOWTIME to loadSound(R.raw.gm_torrefast),
                SoundType.COUNTDOWN3 to loadSound(R.raw.gm_torrefast),
                SoundType.COUNTDOWN2 to loadSound(R.raw.gm_torrefast),
                SoundType.COUNTDOWN1 to loadSound(R.raw.gm_torrefast)
            )
        )
        confirmIds = mapOf(
            SoundPack.LICHESS to loadSound(R.raw.confirmation),
            SoundPack.GM3000 to loadSound(R.raw.gm_start),
            SoundPack.CLASSIC to loadSound(R.raw.gm_start)
        )
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
        val id = packedIds[pack]?.get(SoundType.MOVE) ?: 0
        if (id > 0 && pool != null) {
            try { pool?.play(id, vol(), vol(), 1, 0, pitch()) } catch (_: Exception) { }
        }
        soundPack = previous
    }

    private fun vol(): Float = (volumePercent / 100f).coerceIn(0f, 1f)
    private fun pitch(): Float = when (soundPack) {
        SoundPack.LICHESS -> 1f
        SoundPack.GM3000 -> 1f
        SoundPack.CLASSIC -> 0.75f
    }

    fun play(type: SoundType) {
        if (!enabled) return
        // Sonido de selección de pieza desactivado en todos los paquetes
        if (type == SoundType.CLICK) return

        val id = packedIds[soundPack]?.get(type) ?: 0

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
    fun playConfirmation() {
        if (!enabled) return
        val id = confirmIds[soundPack] ?: 0
        if (id > 0) {
            try {
                pool?.play(id, vol(), vol(), 1, 0, pitch())
            } catch (e: Exception) {
                Log.e("SoundManager", "Failed to play confirmation", e)
            }
        } else {
            Log.w("SoundManager", "Confirmation sound ID is invalid for pack $soundPack")
        }
    }

    fun release() {
        try { pool?.release() } catch (_: Exception) {}
    }
}
