package com.bchess.android.data

import java.util.Date

/**
 * Calculadora de estadísticas que replica EXACTAMENTE la lógica de B-Chess Desktop:
 * - js/profile.js: computeStats(), myResultFrom(), normalizeRecord()
 * - js/stats.js: optionSummary(), optionFlow(), optionOpenings(), optionProgress(), optionColorForm()
 * 
 * Todo en Kotlin puro, sin ECharts ni JavaScript.
 */
object StatsCalculator {

    // --- Punto de entrada principal ---
    fun computeAll(records: List<GameRecord>): ProfileStats {
        val extended = records.map { GameRecordExtended.fromGameRecord(it) }
        return ProfileStats(
            summary = computeSummary(extended),
            flow = computeFlow(extended),
            openings = computeOpenings(extended),
            progress = computeProgress(extended),
            colorForm = computeColorForm(extended)
        )
    }

    // ============================================================
    // 1. RESUMEN GENERAL (computeStats de profile.js + chips de stats.js)
    // ============================================================
    private fun computeSummary(records: List<GameRecordExtended>): StatsSummary {
        if (records.isEmpty()) {
            return StatsSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        }

        var wins = 0
        var draws = 0
        var losses = 0
        var totalMoves = 0L
        var streak = 0
        var bestStreak = 0

        // Para rachas: iterar del más antiguo al más nuevo (reverse chronological)
        records.reversed().forEach { r ->
            val result = r.localWon
            when (result) {
                true -> { wins++; streak = if (streak > 0) streak + 1 else 1 }
                false -> { losses++; streak = if (streak < 0) streak - 1 else -1 }
                null -> { draws++; streak = 0 }
            }
            bestStreak = maxOf(bestStreak, streak)
            totalMoves += r.movesSan.size
        }
        val currentStreak = streak

        val total = records.size
        val winRate = if (total > 0) (wins * 100) / total else 0
        val whites = records.count { it.localColorWhite }
        val blacks = total - whites
        val aiGames = records.count { it.gameType == GAME_TYPE_PRACTICE }
        val onlineGames = total - aiGames
        val avgMoves = if (total > 0) (totalMoves / total).toInt() else 0

        return StatsSummary(
            total = total,
            wins = wins,
            draws = draws,
            losses = losses,
            winRate = winRate,
            whites = whites,
            blacks = blacks,
            aiGames = aiGames,
            onlineGames = onlineGames,
            bestStreak = bestStreak,
            currentStreak = currentStreak,
            totalMoves = totalMoves,
            avgMovesPerGame = avgMoves
        )
    }

    // ============================================================
    // 2. FLUJO (Sankey): color -> resultado -> motivo
    // Replica optionFlow() de stats.js
    // ============================================================
    private fun computeFlow(records: List<GameRecordExtended>): StatsFlow {
        // Nodos base (igual que Desktop)
        val nodes = mutableMapOf<String, FlowNode>()
        fun addNode(name: String, color: String) {
            if (!nodes.contains(name)) nodes[name] = FlowNode(name, color)
        }

        // Colores fijos para categorías
        addNode("Blancas", StatsColors.WHITE)
        addNode("Negras", StatsColors.BLACK)
        addNode("Ganadas", StatsColors.WIN)
        addNode("Perdidas", StatsColors.LOSS)
        addNode("Empates", StatsColors.DRAW)
        addNode("Mate", StatsColors.MATE)
        addNode("Tiempo", StatsColors.TIME)
        addNode("Abandono", StatsColors.RESIGN)
        addNode("Otro", StatsColors.OTHER)

        // Primeras jugadas que aparecerán como nodos extra
        val firstMoves = mutableSetOf<String>()

        val counts = mutableMapOf<String, Int>()
        fun bump(a: String, b: String) {
            val key = "$a→$b"
            counts[key] = (counts[key] ?: 0) + 1
        }

        records.forEach { r ->
            // Primera jugada (SAN limpio)
            val mv = firstMoveSAN(r.movesSan)
            if (mv != null && mv != "Otra") {
                firstMoves.add(mv)
                bump(mv, if (r.localColorWhite) "Blancas" else "Negras")
            }
            // Color -> Resultado
            val colorNode = if (r.localColorWhite) "Blancas" else "Negras"
            val resultNode = when (r.localWon) {
                true -> "Ganadas"
                false -> "Perdidas"
                null -> "Empates"
            }
            bump(colorNode, resultNode)
            // Resultado -> Motivo
            val reason = reasonKind(r.endReason)
            bump(resultNode, reason)
        }

        // Añadir nodos de primeras jugadas
        firstMoves.forEach { mv ->
            val color = when (mv) {
                "e4" -> StatsColors.WHITE
                "d4" -> StatsColors.WIN
                else -> StatsColors.BLACK
            }
            addNode(mv, color)
        }

        // Construir links
        val links = counts.map { (k, v) ->
            val parts = k.split("→")
            FlowLink(parts[0], parts[1], v)
        }.toList()

        return StatsFlow(nodes.values.toList(), links)
    }

    private fun firstMoveSAN(movesSan: List<String>): String? {
        if (movesSan.isEmpty()) return null
        val san = movesSan[0].replace(Regex("[+#]"), "")
        return when (san) {
            "e4", "d4", "c4", "Nf3", "f4", "g3", "b3" -> san
            else -> "Otra"
        }
    }

    private fun reasonKind(reason: String?): String {
        val r = reason?.lowercase() ?: ""
        return when {
            r.contains("mate") || r.contains("jaque") -> "Mate"
            r.contains("tiempo") || r.contains("timeout") -> "Tiempo"
            r.contains("rendi") || r.contains("abandono") || r.contains("desconec") -> "Abandono"
            r.contains("tablas") || r.contains("acuerdo") || r.contains("draw") -> "Acuerdo"
            else -> "Otro"
        }
    }

    // ============================================================
    // 3. APERTURAS (árbol w1 -> b1 -> w2 -> b2)
    // Replica optionOpenings() de stats.js
    // ============================================================
    private fun computeOpenings(records: List<GameRecordExtended>): StatsOpenings {
        // Estructura: root[w1][b1][w2][b2] = count
        val root = mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, Int>>>>()

        records.forEach { r ->
            val sans = r.movesSan.take(6)
            if (sans.size < 2) return@forEach

            val w1 = sans[0].replace(Regex("[+#]"), "")
            val b1 = sans[1].replace(Regex("[+#]"), "")
            val w2 = if (sans.size > 2) sans[2].replace(Regex("[+#]"), "") else null
            val b2 = if (sans.size > 3) sans[3].replace(Regex("[+#]"), "") else null

            val l1 = root.getOrPut(w1) { mutableMapOf() }
            val l2 = l1.getOrPut(b1) { mutableMapOf() }
            val l3 = l2.getOrPut(w2 ?: "") { mutableMapOf() }
            val current = l3.getOrDefault(b2 ?: "", 0)
            l3.put(b2 ?: "", current + 1)
        }

        if (root.isEmpty()) return StatsOpenings(emptyList(), 1)

        // Calcular maxCount global para normalizar colores
        var maxCount = 1
        root.values.forEach { l1 ->
            l1.values.forEach { l2 ->
                l2.values.forEach { l3 ->
                    l3.values.forEach { count ->
                        maxCount = maxOf(maxCount, count)
                    }
                }
            }
        }

        // Construir árbol para visualización
        val nodes = root.entries.map { (w1, l1) ->
            val l1Total = l1.values.flatMap { it.values.flatMap { it.values } }.sum()
            OpeningNode(
                name = "1. $w1",
                value = l1Total,
                color = StatsColors.heatColor(l1Total, maxCount),
                children = l1.entries
                    .sortedByDescending { it.value.values.flatMap { it.values }.sum() }
                    .take(5)
                    .map { (b1, l2) ->
                        val l2Total = l2.values.flatMap { it.values }.sum()
                        OpeningNode(
                            name = "1... $b1",
                            value = l2Total,
                            color = StatsColors.heatColor(l2Total, maxCount),
                            children = l2.entries
                                .filter { it.key.isNotBlank() }
                                .sortedByDescending { it.value.values.sum() }
                                .take(4)
                                .map { (w2, l3) ->
                                    val l3Total = l3.values.sum()
                                    OpeningNode(
                                        name = "2. $w2",
                                        value = l3Total,
                                        color = StatsColors.heatColor(l3Total, maxCount),
                                        children = l3.entries
                                            .filter { it.key.isNotBlank() }
                                            .sortedByDescending { it.value }
                                            .take(3)
                                            .map { (b2, count) ->
                                                OpeningNode(
                                                    name = "2... $b2",
                                                    value = count,
                                                    color = StatsColors.heatColor(count, maxCount)
                                                )
                                            }
                                    )
                                }
                        )
                    }
            )
        }

        return StatsOpenings(nodes, maxCount)
    }

    // ============================================================
    // 4. PROGRESO (% victorias acumulado por partida)
    // Replica optionProgress() de stats.js
    // ============================================================
    private fun computeProgress(records: List<GameRecordExtended>): StatsProgress {
        if (records.isEmpty()) return StatsProgress(emptyList())

        // Orden cronológico (más antigua primero)
        val chrono = records.sortedBy { it.dateMs }

        fun seriesFor(filter: (GameRecordExtended) -> Boolean): List<Double?> {
            var runningN = 0
            var runningW = 0
            val pts = mutableListOf<Double?>()
            chrono.forEach { g ->
                if (!filter(g)) {
                    pts.add(null)
                    return@forEach
                }
                runningN++
                if (g.localWon == true) runningW++
                pts.add(if (runningN > 0) (runningW.toDouble() / runningN * 100.0) else null)
            }
            return pts
        }

        val allSeries = seriesFor { true }
        val aiSeries = seriesFor { it.gameType == GAME_TYPE_PRACTICE }
        val onlineSeries = seriesFor { it.gameType != GAME_TYPE_PRACTICE }

        val points = allSeries.indices.map { i ->
            ProgressPoint(
                gameIndex = i + 1,
                totalWinRate = allSeries[i],
                aiWinRate = aiSeries[i],
                onlineWinRate = onlineSeries[i]
            )
        }

        return StatsProgress(points)
    }

    // ============================================================
    // 5. RESULTADOS POR COLOR (6 series acumuladas)
    // Replica optionColorForm() de stats.js
    // ============================================================
    private fun computeColorForm(records: List<GameRecordExtended>): StatsColorForm {
        if (records.isEmpty()) return StatsColorForm(emptyList())

        val chrono = records.sortedBy { it.dateMs }
        val isWhite = { g: GameRecordExtended -> g.localColorWhite }
        val isBlack = { g: GameRecordExtended -> !g.localColorWhite }
        val win = { g: GameRecordExtended -> g.localWon == true }
        val draw = { g: GameRecordExtended -> g.localWon == null }
        val loss = { g: GameRecordExtended -> g.localWon == false }

        fun seriesDef(name: String, color: String, dash: Boolean, filter: (GameRecordExtended) -> Boolean): ColorFormSeries {
            var acc = 0
            val pts = mutableListOf<Int>()
            chrono.forEach { g ->
                if (filter(g)) acc++
                pts.add(acc)
            }
            return ColorFormSeries(name, color, dash, pts)
        }

        val series = listOf(
            seriesDef("Ganadas blancas", "#2ea043", false, { isWhite(it) && win(it) }),
            seriesDef("Empates blancas", "#d29922", false, { isWhite(it) && draw(it) }),
            seriesDef("Perdidas blancas", "#f85149", false, { isWhite(it) && loss(it) }),
            seriesDef("Ganadas negras", "#3fb950", true, { isBlack(it) && win(it) }),
            seriesDef("Empates negras", "#eac54f", true, { isBlack(it) && draw(it) }),
            seriesDef("Perdidas negras", "#ff7b72", true, { isBlack(it) && loss(it) })
        )

        return StatsColorForm(series)
    }
}