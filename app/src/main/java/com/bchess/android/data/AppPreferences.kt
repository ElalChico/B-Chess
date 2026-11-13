package com.bchess.android.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.graphics.Color

enum class BoardTheme(
    val labelEs: String,
    val labelEn: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val boardBackground: Color,
    val panelBackground: Color
) {
    LICHESS(
        "Marrón Lichess",
        "Lichess",
        Color(0xFFF0D9B5),
        Color(0xFFB58863),
        Color(0xFF12171D),
        Color(0xFF1A232C)
    ),
    DARK(
        "Oscuro gris",
        "Dark Grey",
        Color(0xFFB6BEC9),
        Color(0xFF5D6775),
        Color(0xFF11161B),
        Color(0xFF1A2027)
    ),
    FOREST(
        "Verde profundo",
        "Deep Forest",
        Color(0xFFDDE2C6),
        Color(0xFF4F6B3A),
        Color(0xFF121712),
        Color(0xFF1A231A)
    ),
    STEEL(
        "Acero azul",
        "Steel Blue",
        Color(0xFFD9E2EC),
        Color(0xFF42566B),
        Color(0xFF121720),
        Color(0xFF1A2330)
    ),
    PARCHMENT(
        "Pergamino",
        "Parchment",
        Color(0xFFF2E4C9),
        Color(0xFF83644A),
        Color(0xFF171310),
        Color(0xFF211C17)
    ),
    BURGUNDY(
        "Burdeos",
        "Burgundy",
        Color(0xFFE6D3DC),
        Color(0xFF5C313E),
        Color(0xFF171113),
        Color(0xFF221A1F)
    ),
    IVORY(
        "Marfil",
        "Ivory",
        Color(0xFFF7F5EC),
        Color(0xFF4A463E),
        Color(0xFF131312),
        Color(0xFF1D1D1B)
    )
}

enum class PieceSet(
    val labelEs: String,
    val labelEn: String,
    val resKey: String? = null, // clave para drawables ps_<resKey>_<w|b><pieza> (PIEZAS); null = drawables piece_* existentes
) {
    LICHESS("Lichess · Vector", "Lichess · Vector"),
    CHESS_COM("Moderno", "Modern"),
    OPEN_SOURCE("Trazo abierto", "Open line"),

    // --- 12 nuevos conjuntos desde PIEZAS ---
    ALPHA("Alpha", "Alpha", "alpha"),
    ANARCANDY("Anarcandy", "Anarcandy", "anarcandy"),
    // Variante blanca de Anarcandy: rosa -> blanco, mantiene los acentos
    ANARCANDY_WHITE("Anarcandy · Blanco", "Anarcandy · White", "anarcandywhite"),
    // Variante propia de gatos punk/anarquista
    CATPUNK("Gatos Punk", "Punk Cats", "catpunk"),
    CBURNETT("Lichess · Clásico", "Lichess · Classic", "cburnett"),
    COMPANION("Cómplice", "Companion", "companion"),
    HORSEY("Horsey", "Horsey", "horsey"),
    ICPIECES("ICPieces", "ICPieces", "icpieces"),
    LEIPZIG("Leipzig", "Leipzig", "leipzig"),
    MERIDA("Merida", "Merida", "merida"),
    PIXEL("Pixel", "Pixel", "pixel"),
    RIOHACHA("Riohacha", "Riohacha", "riohacha"),
    STAUNTY("Staunty", "Staunty", "staunty"),
    TATIANA("Tatiana", "Tatiana", "tatiana");

    companion object {
        val DEFAULT = LICHESS
    }
}

enum class SoundPack(val labelEs: String, val labelEn: String) {
    LICHESS("Lichess", "Lichess"),
    FREE("Gratis / SFX", "Free / SFX"),
    CLASSIC("Clásico", "Classic");

    companion object {
        val DEFAULT = LICHESS
    }
}

enum class AppLanguage(val code: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English")
}

data class PrivacySettings(
    val sharePhoto: Boolean = true,
    val shareHistory: Boolean = true,
    val sharePgn: Boolean = true
)

/**
 * Lightweight persisted preferences (SharedPreferences) — no extra dependency.
 */
class AppPreferences(context: Context) {
    private val p = context.getSharedPreferences("chessbt_prefs", Context.MODE_PRIVATE)

    private var _soundEnabled by mutableStateOf(p.getBoolean("sound", true))
    val soundEnabled: Boolean get() = _soundEnabled

    private var _soundVolume by mutableIntStateOf(p.getInt("volume", 80))
    val soundVolume: Int get() = _soundVolume

    private var _soundPack by mutableStateOf(
        runCatching { SoundPack.valueOf(p.getString("sound_pack", SoundPack.DEFAULT.name)!!) }.getOrDefault(SoundPack.DEFAULT)
    )
    val soundPack: SoundPack get() = _soundPack

    private var _boardTheme by mutableStateOf(
        runCatching { BoardTheme.valueOf(p.getString("theme", BoardTheme.LICHESS.name)!!) }.getOrDefault(BoardTheme.LICHESS)
    )
    val boardTheme: BoardTheme get() = _boardTheme

    private var _pieceSet by mutableStateOf(
        runCatching { PieceSet.valueOf(p.getString("piece_set", PieceSet.DEFAULT.name)!!) }.getOrDefault(PieceSet.DEFAULT)
    )
    val pieceSet: PieceSet get() = _pieceSet

    private var _language by mutableStateOf(
        runCatching { AppLanguage.valueOf(p.getString("lang", AppLanguage.ES.name)!!) }.getOrDefault(AppLanguage.ES)
    )
    val language: AppLanguage get() = _language

    private var _privacy by mutableStateOf(
        PrivacySettings(
            sharePhoto = p.getBoolean("share_photo", true),
            shareHistory = p.getBoolean("share_history", true),
            sharePgn = p.getBoolean("share_pgn", true)
        )
    )
    val privacy: PrivacySettings get() = _privacy

    private var _profileName by mutableStateOf(p.getString("profile_name", "Jugador") ?: "Jugador")
    val profileName: String get() = _profileName

    private var _profileConfigured by mutableStateOf(
        p.getBoolean("profile_configured", p.getString("profile_name", null) != null)
    )
    val profileConfigured: Boolean get() = _profileConfigured

    private var _profilePhotoBase64 by mutableStateOf(p.getString("profile_photo", "") ?: "")
    val profilePhotoBase64: String get() = _profilePhotoBase64

    private var _defaultTimeMs by mutableLongStateOf(p.getLong("time_ms", 300_000L))
    val defaultTimeMs: Long get() = _defaultTimeMs

    private var _defaultIncrementMs by mutableLongStateOf(p.getLong("inc_ms", 0L))
    val defaultIncrementMs: Long get() = _defaultIncrementMs

    private var _checkSignal by mutableStateOf(p.getBoolean("check_sig", true))
    val checkSignal: Boolean get() = _checkSignal

    private var _moveIndicators by mutableStateOf(p.getBoolean("move_ind", true))
    val moveIndicators: Boolean get() = _moveIndicators

    private var _showCoordinates by mutableStateOf(p.getBoolean("show_coordinates", true))
    val showCoordinates: Boolean get() = _showCoordinates

    fun setSoundEnabled(v: Boolean) { _soundEnabled = v; p.edit().putBoolean("sound", v).apply() }
    fun setSoundVolume(v: Int) { val c = v.coerceIn(0, 100); _soundVolume = c; p.edit().putInt("volume", c).apply() }
    fun setSoundPack(v: SoundPack) { _soundPack = v; p.edit().putString("sound_pack", v.name).apply() }
    fun setBoardTheme(v: BoardTheme) { _boardTheme = v; p.edit().putString("theme", v.name).apply() }
    fun setPieceSet(v: PieceSet) { _pieceSet = v; p.edit().putString("piece_set", v.name).apply() }
    fun setLanguage(v: AppLanguage) { _language = v; p.edit().putString("lang", v.name).apply() }
    fun setPrivacy(v: PrivacySettings) {
        _privacy = v
        p.edit().putBoolean("share_photo", v.sharePhoto)
            .putBoolean("share_history", v.shareHistory)
            .putBoolean("share_pgn", v.sharePgn).apply()
    }
    fun setProfileName(v: String) { val n = v.trim().ifBlank { "Jugador" }.take(20); _profileName = n; p.edit().putString("profile_name", n).apply() }
    fun completeProfile() { _profileConfigured = true; p.edit().putBoolean("profile_configured", true).apply() }
    fun setProfilePhotoBase64(v: String) {
        val clean = v.trim()
        // Nunca guardar un base64 truncado o incompleto: al decodificarlo
        // produciria una foto rota o cortada. Si no es valido, se descarta.
        val valid = clean.isBlank() || isDecodablePhoto(clean)
        _profilePhotoBase64 = if (valid) clean.take(200_000) else ""
        p.edit().putString("profile_photo", _profilePhotoBase64).apply()
    }

    /** Comprueba que el base64 sea una imagen completa y decodificable. */
    private fun isDecodablePhoto(value: String): Boolean {
        val raw = value.substringAfter("base64,", value)
        if (raw.isBlank() || raw.length % 4 != 0) return false
        return try {
            val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
            bytes.isNotEmpty() &&
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null
        } catch (_: Exception) {
            false
        }
    }
    fun setDefaultTime(timeMs: Long, incMs: Long) {
        _defaultTimeMs = timeMs.coerceAtLeast(0); _defaultIncrementMs = incMs.coerceIn(0, 60_000)
        p.edit().putLong("time_ms", _defaultTimeMs).putLong("inc_ms", _defaultIncrementMs).apply()
    }
    fun setCheckSignal(v: Boolean) { _checkSignal = v; p.edit().putBoolean("check_sig", v).apply() }
    fun setMoveIndicators(v: Boolean) { _moveIndicators = v; p.edit().putBoolean("move_ind", v).apply() }
    fun setShowCoordinates(v: Boolean) { _showCoordinates = v; p.edit().putBoolean("show_coordinates", v).apply() }

    fun t(es: String, en: String): String = if (language == AppLanguage.EN) en else es
}
