package com.bchess.android.data

import com.bchess.android.core.ChessEngine
import com.bchess.android.core.Move
import com.bchess.android.core.PieceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PgnArchive {
    const val MAX_ARCHIVE_CHARS = 5_000_000
    private val headerRegex = Regex("""(?m)^\[([A-Za-z0-9_]+)\s+"((?:\\.|[^"])*)"\]\s*$""")
    private val eventRegex = Regex("""(?m)^\[Event\s""")
    private val destinationRegex = Regex("""([a-h][1-8])(?:=?([QRBN]))?$""")
    private val moveNumberRegex = Regex("""^\d+\.(?:\.\.)?""")
    private val resultTokens = setOf("1-0", "0-1", "1/2-1/2", "*")

    fun parse(text: String): List<GameRecord> {
        require(text.isNotBlank()) { "El archivo PGN está vacío." }
        require(text.length <= MAX_ARCHIVE_CHARS) { "El archivo PGN supera el tamaño máximo permitido." }
        val starts = eventRegex.findAll(text).map { it.range.first }.toList()
        val games = if (starts.isEmpty()) {
            listOf(text)
        } else {
            starts.mapIndexed { index, start ->
                text.substring(start, starts.getOrNull(index + 1) ?: text.length)
            }
        }

        return games.mapNotNull { game ->
            if (game.isBlank()) null else parseGame(game)
        }.also {
            require(it.isNotEmpty()) { "No se encontraron partidas PGN válidas." }
        }
    }

    private fun parseGame(text: String): GameRecord {
        val tags = headerRegex.findAll(text).associate { match ->
            match.groupValues[1] to unescapeTag(match.groupValues[2])
        }
        require(tags["SetUp"] != "1" && tags["FEN"].isNullOrBlank()) {
            "Solo se pueden cargar partidas PGN que comienzan desde la posición inicial."
        }
        require(tags["Variant"].isNullOrBlank() || tags["Variant"] in setOf("Standard", "Normal")) {
            "Este reproductor solo admite partidas de ajedrez estándar."
        }
        val moveText = headerRegex.replace(text, " ")
        val tokens = tokenize(moveText)
        val result = tags["Result"] ?: tokens.lastOrNull { it in resultTokens }
            ?: throw IllegalArgumentException("La partida PGN no indica un resultado.")
        require(result in setOf("1-0", "0-1", "1/2-1/2")) {
            "Solo se pueden cargar partidas PGN finalizadas."
        }

        val sanMoves = tokens
            .filterNot { it in resultTokens }
            .map(::cleanSan)
            .filterNot { it.isBlank() || it == "e.p." }
        val engine = ChessEngine()
        val encodedUci = tags["CHESSXUCI"]
            ?.split(Regex("\\s+"))
            ?.filter(String::isNotBlank)
        val uciMoves = if (encodedUci != null) {
            require(encodedUci.size == sanMoves.size) {
                "El archivo PGN tiene una lista de movimientos CHESS-X incompleta."
            }
            encodedUci.map { uci ->
                val move = findUciMove(engine, uci)
                    ?: throw IllegalArgumentException("El archivo PGN contiene un movimiento UCI no válido: $uci")
                require(engine.doMove(move)) { "No se pudo aplicar un movimiento del archivo PGN." }
                move.uci()
            }
        } else {
            sanMoves.map { san ->
                val move = findSanMove(engine, san)
                    ?: throw IllegalArgumentException("No se pudo interpretar el movimiento PGN: $san")
                require(engine.doMove(move)) { "No se pudo aplicar el movimiento PGN: $san" }
                move.uci()
            }
        }

        val white = tags["White"].orEmpty().ifBlank { "Blancas" }
        val black = tags["Black"].orEmpty().ifBlank { "Negras" }
        val timeControl = parseTimeControl(tags["TimeControl"].orEmpty())
        val dateMs = parseDate(tags["Date"])
        val record = GameRecord(
            dateMs = dateMs,
            opponent = black,
            result = result,
            localWon = when (result) {
                "1-0" -> true
                "0-1" -> false
                else -> null
            },
            localColorWhite = true,
            timeLabel = timeControl,
            movesSan = sanMoves,
            pgn = "",
            finalFen = engine.toFEN(),
            playerName = white,
            uciMoves = uciMoves
        )
        return record.copy(pgn = buildPgn(record, white, black))
    }

    private fun tokenize(text: String): List<String> {
        val withoutCommentsAndVariations = StringBuilder()
        var comment = false
        var variationDepth = 0
        var lineComment = false
        for (char in text) {
            if (lineComment) {
                if (char == '\n') {
                    lineComment = false
                    if (variationDepth == 0) withoutCommentsAndVariations.append(char)
                }
                continue
            }
            if (comment) {
                if (char == '}') comment = false
                continue
            }
            when (char) {
                '{' -> comment = true
                ';' -> lineComment = true
                '(' -> variationDepth++
                ')' -> {
                    require(variationDepth > 0) { "El archivo PGN contiene una variación sin abrir." }
                    variationDepth--
                }
                else -> if (variationDepth == 0) withoutCommentsAndVariations.append(char)
            }
        }
        require(!comment && variationDepth == 0) { "El archivo PGN contiene comentarios o variaciones incompletos." }

        return withoutCommentsAndVariations.toString()
            .split(Regex("\\s+"))
            .mapNotNull { raw ->
                val token = raw.trim().replace(moveNumberRegex, "")
                when {
                    token.isBlank() || token.startsWith("$") -> null
                    token.matches(Regex("""\d+\.{1,3}""")) -> null
                    else -> token
                }
            }
    }

    private fun cleanSan(token: String): String =
        token.replace(Regex("[!?+#]+$"), "")
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")

    private fun findUciMove(engine: ChessEngine, uci: String): Move? {
        if (!uci.matches(Regex("""[a-h][1-8][a-h][1-8][qrbn]?"""))) return null
        val from = Move.algebraicToSquare(uci.substring(0, 2)) ?: return null
        val to = Move.algebraicToSquare(uci.substring(2, 4)) ?: return null
        val promotion = when (uci.getOrNull(4)) {
            'q' -> PieceType.QUEEN
            'r' -> PieceType.ROOK
            'b' -> PieceType.BISHOP
            'n' -> PieceType.KNIGHT
            else -> null
        }
        return engine.findMove(from, to, promotion)
    }

    private fun findSanMove(engine: ChessEngine, san: String): Move? {
        if (san == "O-O" || san == "O-O-O") {
            return engine.legalMoves().singleOrNull {
                if (san == "O-O") it.isCastleKingside else it.isCastleQueenside
            }
        }
        val destination = destinationRegex.find(san) ?: return null
        val target = Move.algebraicToSquare(destination.groupValues[1]) ?: return null
        val promotion = when (destination.groupValues.getOrNull(2)) {
            "Q" -> PieceType.QUEEN
            "R" -> PieceType.ROOK
            "B" -> PieceType.BISHOP
            "N" -> PieceType.KNIGHT
            else -> null
        }
        val prefix = san.substring(0, destination.range.first)
        val pieceLetter = prefix.firstOrNull()?.takeIf { it in "KQRBN" }
        val pieceType = when (pieceLetter) {
            'K' -> PieceType.KING
            'Q' -> PieceType.QUEEN
            'R' -> PieceType.ROOK
            'B' -> PieceType.BISHOP
            'N' -> PieceType.KNIGHT
            else -> PieceType.PAWN
        }
        val originHint = prefix.removePrefix(pieceLetter?.toString().orEmpty()).replace("x", "")
        val isCapture = 'x' in prefix

        return engine.legalMoves().filter { move ->
            val piece = engine.pieceAt(move.from)
            piece?.type == pieceType &&
                move.to == target &&
                move.isCapture == isCapture &&
                move.promotion == promotion &&
                originHint.all { hint ->
                    when {
                        hint in 'a'..'h' -> 'a' + (move.from % 8) == hint
                        hint in '1'..'8' -> '1' + (move.from / 8) == hint
                        else -> false
                    }
                }
        }.singleOrNull()
    }

    private fun parseDate(date: String?): Long {
        if (date.isNullOrBlank() || date == "????.??.??") return System.currentTimeMillis()
        return SimpleDateFormat("yyyy.MM.dd", Locale.US).apply { isLenient = false }
            .parse(date)?.time ?: System.currentTimeMillis()
    }

    private fun parseTimeControl(value: String): String {
        val match = Regex("""^(\d+)\+(\d+)$""").matchEntire(value.trim()) ?: return value.ifBlank { "5 + 0" }
        val seconds = match.groupValues[1].toLongOrNull() ?: return value
        val increment = match.groupValues[2]
        return "${seconds / 60} + $increment"
    }

    private fun unescapeTag(value: String): String =
        value.replace("\\\"", "\"").replace("\\\\", "\\")
}
