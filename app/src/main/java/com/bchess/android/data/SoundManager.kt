package com.bchess.android.data

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.bchess.android.R

enum class SoundType {
    MOVE, CAPTURE, CHECK, CHECKMATE, ILLEGAL, CLICK, WIN, LOSE, DRAW, PAUSE, LOWTIME, COUNTDOWN3, COUNTDOWN2, COUNTDOWN1
}

/**
 * Motor de sonidos (MediaPlayer, robusto en todos los dispositivos).
 *
 * Paquetes (cada uno con archivos reales distintos):
 *  - LICHESS: pack estándar de Lichess (MP3 incluidos en la app).
 *  - GM3000:  librería de efectos GM-3000 (carpeta public/sounds del proyecto GM-3000).
 *  - CLASSIC: los mismos efectos GM-3000 pero pitch 0.75 para un carácter clásico.
 *
 * El sonido de selección de pieza (CLICK) está desactivado en los tres paquetes.
 */
class SoundManager(private val context: Context, pack: SoundPack = SoundPack.DEFAULT) {
    private var enabled: Boolean = true
    private var volumePercent: Int = 80
    private var soundPack: SoundPack = pack

    private val resourceIds: Map<SoundPack, Map<SoundType, Int>> = mapOf(
        SoundPack.LICHESS to mapOf(
            SoundType.MOVE to R.raw.move,
            SoundType.CAPTURE to R.raw.capture,
            SoundType.CHECK to R.raw.check,
            SoundType.CHECKMATE to R.raw.checkmate,
            SoundType.ILLEGAL to R.raw.error,
            SoundType.WIN to R.raw.victory,
            SoundType.LOSE to R.raw.defeat,
            SoundType.DRAW to R.raw.draw,
            SoundType.PAUSE to R.raw.move,
            SoundType.LOWTIME to R.raw.lowtime,
            SoundType.COUNTDOWN3 to R.raw.countdown3,
            SoundType.COUNTDOWN2 to R.raw.countdown2,
            SoundType.COUNTDOWN1 to R.raw.countdown1
        ),
        SoundPack.GM3000 to mapOf(
            SoundType.MOVE to R.raw.gm_move,
            SoundType.CAPTURE to R.raw.gm_capture,
            SoundType.CHECK to R.raw.gm_jaque,
            SoundType.CHECKMATE to R.raw.gm_mate,
            SoundType.ILLEGAL to R.raw.gm_error,
            SoundType.WIN to R.raw.gm_victory,
            SoundType.LOSE to R.raw.gm_tiempofinalizado,
            SoundType.DRAW to R.raw.gm_draw,
            SoundType.PAUSE to R.raw.gm_save,
            SoundType.LOWTIME to R.raw.gm_torrefast,
            SoundType.COUNTDOWN3 to R.raw.gm_torrefast,
            SoundType.COUNTDOWN2 to R.raw.gm_torrefast,
            SoundType.COUNTDOWN1 to R.raw.gm_torrefast
        ),
        SoundPack.CLASSIC to mapOf(
            SoundType.MOVE to R.raw.gm_move,
            SoundType.CAPTURE to R.raw.gm_capture,
            SoundType.CHECK to R.raw.gm_jaque,
            SoundType.CHECKMATE to R.raw.gm_mate,
            SoundType.ILLEGAL to R.raw.gm_error,
            SoundType.WIN to R.raw.gm_victory,
            SoundType.LOSE to R.raw.gm_tiempofinalizado,
            SoundType.DRAW to R.raw.gm_draw,
            SoundType.PAUSE to R.raw.gm_save,
            SoundType.LOWTIME to R.raw.gm_torrefast,
            SoundType.COUNTDOWN3 to R.raw.gm_torrefast,
            SoundType.COUNTDOWN2 to R.raw.gm_torrefast,
            SoundType.COUNTDOWN1 to R.raw.gm_torrefast
        )
    )

    private val confirmationIds: Map<SoundPack, Int> = mapOf(
        SoundPack.LICHESS to R.raw.confirmation,
        SoundPack.GM3000 to R.raw.gm_start,
        SoundPack.CLASSIC to R.raw.gm_start
    )

    private val clockIds: Map<SoundType, Int> = mapOf(
        SoundType.LOWTIME to R.raw.lowtime,
        SoundType.COUNTDOWN3 to R.raw.countdown3,
        SoundType.COUNTDOWN2 to R.raw.countdown2,
        SoundType.COUNTDOWN1 to R.raw.countdown1
    )

    fun setEnabled(enabled: Boolean) { this.enabled = enabled }
    fun isEnabled(): Boolean = enabled
    fun setVolume(percent: Int) { volumePercent = percent.coerceIn(0, 100) }
    fun getVolume(): Int = volumePercent
    fun setPack(pack: SoundPack) { soundPack = pack }

    /** Vista previa de un paquete de sonidos para Ajustes (suena siempre). */
    fun playPreview(pack: SoundPack) {
        val previous = soundPack
        soundPack = pack
        playInternal(resourceIds[pack]?.get(SoundType.MOVE) ?: return)
        soundPack = previous
    }

    private fun vol(): Float = (volumePercent / 100f).coerceIn(0f, 1f)
    private fun playbackRate(): Float = when (soundPack) {
        SoundPack.LICHESS -> 1f
        SoundPack.GM3000 -> 1f
        SoundPack.CLASSIC -> 0.75f
    }

    fun play(type: SoundType) {
        if (!enabled) return
        // CLICK (selección de pieza) desactivado en todos los paquetes
        if (type == SoundType.CLICK) return

        val resId = clockIds[type] ?: resourceIds[soundPack]?.get(type) ?: return
        playInternal(resId)
    }

    /** Sonido de confirmación (aceptar conexiones / iniciar partida). */
    fun playConfirmation() {
        if (!enabled) return
        playInternal(confirmationIds[soundPack] ?: return)
    }

    private fun playInternal(resId: Int) {
        try {
            val mp = MediaPlayer.create(context, resId) ?: run {
                Log.w("SoundManager", "MediaPlayer could not create for resId=$resId")
                return
            }
            val v = vol()
            mp.setVolume(v, v)
            val rate = playbackRate()
            if (rate != 1f) {
                try { mp.playbackParams = mp.playbackParams.setPitch(rate) } catch (_: Exception) { }
            }
            mp.setOnCompletionListener { p -> try { p.release() } catch (_: Exception) { } }
            mp.start()
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to play resId=$resId", e)
        }
    }

    fun release() {
        // Los MediaPlayer se liberan al terminar cada sonido; nada queda reservado.
    }
}
