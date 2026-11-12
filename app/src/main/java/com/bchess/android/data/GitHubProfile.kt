package com.bchess.android.data

/**
 * Datos publicos del desarrollador (GitHub).
 *
 * El avatar se sincroniza siempre desde GitHub para que la app y el repo
 * muestren la misma imagen de perfil sin trabajo manual.
 */
object GitHubProfile {

    /** Perfil de GitHub del desarrollador. */
    const val PROFILE_URL = "https://github.com/ElalChico"

    /** Avatar publico de GitHub (se actualiza solo, sin cache en el repo). */
    const val AVATAR_URL = "https://github.com/ElalChico.png?size=460"

    /** Proyecto hermano: mismo juego pero sin Stockfish. */
    const val RELATED_PROJECT_NAME = "Chess-X"

    const val RELATED_PROJECT_URL = "https://github.com/ElalChico/chess-x"

    /** Descripcion corta del desarrollador. */
    const val BIO =
        "Desarrollador autodidacta | Hacking etico & software libre | " +
            "Comprometido con la justicia, el antiespecismo y la tecnologia " +
            "al servicio de la critica social."
}
