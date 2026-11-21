package com.bchess.android.data

import java.util.Date

/**
 * Modelos de estadísticas que replican EXACTAMENTE los cálculos de B-Chess Desktop (js/profile.js y js/stats.js)
 * No se usa ECharts ni JavaScript: todo en Kotlin/Compose nativo.
 */

// --- Resumen general (para el donut + chips) ---
data class StatsSummary(
    val total: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winRate: Int,
    val whites: Int,
    val blacks: Int,
    val aiGames: Int,
    val onlineGames: Int,
    val bestStreak: Int,
    val currentStreak: Int,
    val totalMoves: Long,
    val avgMovesPerGame: Int
) {
    fun winRatePercent(): String = "$winRate%"
    fun streakDisplay(): String = when {
        currentStreak > 0 -> "+$currentStreak"
        currentStreak < 0 -> "$currentStreak"
        else -> "0"
    }
    fun bestStreakDisplay(): String = if (bestStreak > 0) "+$bestStreak" else "0"
}

// --- Flujo (Sankey): color -> resultado -> motivo ---
data class FlowNode(
    val name: String,
    val color: String, // hex color
    val value: Int = 0 // partidas que pasan por este nodo
)

data class FlowLink(
    val source: String,
    val target: String,
    val value: Int
)

data class StatsFlow(
    val nodes: List<FlowNode>,
    val links: List<FlowLink>
)

// --- Aperturas (árbol hasta 4 ply: w1 -> b1 -> w2 -> b2) ---
data class OpeningNode(
    val name: String,           // ej: "1. e4", "1... c5", "2. Nf3"
    val value: Int,             // partidas con esta secuencia
    val color: String,          // heat color (azul -> rojo según frecuencia)
    val children: List<OpeningNode> = emptyList()
)

data class StatsOpenings(
    val roots: List<OpeningNode>,
    val maxCount: Int // para normalizar colores
)

// --- Progreso (% victorias acumulado) ---
data class ProgressPoint(
    val gameIndex: Int,      // 1-based
    val totalWinRate: Double?,    // % todas
    val aiWinRate: Double?,       // % vs IA
    val onlineWinRate: Double?    // % en línea
)

data class StatsProgress(
    val points: List<ProgressPoint>
)

// --- Resultados por color (6 series acumuladas) ---
data class ColorFormSeries(
    val name: String,
    val color: String,
    val dash: Boolean, // true = negras (discontinuous), false = blancas (sólida)
    val data: List<Int> // acumulado por partida
)

data class StatsColorForm(
    val series: List<ColorFormSeries>
)

// --- Contenedor principal de todas las estadísticas ---
data class ProfileStats(
    val summary: StatsSummary,
    val flow: StatsFlow,
    val openings: StatsOpenings,
    val progress: StatsProgress,
    val colorForm: StatsColorForm,
    val lastCalculated: Date = Date()
)

// --- Utilidades de color (igual que Desktop) ---
object StatsColors {
    val WIN = "#2ea043"
    val LOSS = "#f85149"
    val DRAW = "#d29922"
    val WHITE = "#58a6ff"
    val BLACK = "#a371f7"
    val AI = "#f0883e"
    val ONLINE = "#39d2c0"
    val MATE = "#f0883e"
    val TIME = "#39d2c0"
    val RESIGN = "#db61a2"
    val OTHER = "#8b949e"

    // Heat map para aperturas (azul -> celeste -> amarillo -> naranja -> rojo)
    fun heatColor(value: Int, maxCount: Int): String {
        val t = if (maxCount > 0) (value.toDouble() / maxCount).coerceIn(0.0, 1.0) else 0.0
        return when {
            t < 0.2 -> "#58a6ff"   // azul - poco usada
            t < 0.4 -> "#79c0ff"   // celeste - algo usada
            t < 0.65 -> "#eac54f"  // amarillo - media
            t < 0.85 -> "#f0883e"  // naranja - muy usada
            else -> "#f85149"      // rojo - la más usada
        }
    }

    // Colores para flujo (motivos de fin)
    fun reasonColor(reason: String): String = when (reason.lowercase()) {
        "mate", "jaque mate", "checkmate" -> MATE
        "tiempo", "timeout", "time" -> TIME
        "rendi", "abandono", "resign", "desconec" -> RESIGN
        "tablas", "acuerdo", "draw", "agreed" -> DRAW
        else -> OTHER
    }
}