package com.chessx.data

import com.chessx.core.ChessEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnArchiveTest {
    @Test
    fun parsesStandardSanAndBuildsReplayableMoves() {
        val record = PgnArchive.parse(
            """
            [Event "Casual"]
            [Date "2025.02.03"]
            [White "Ana"]
            [Black "Luis"]
            [Result "1-0"]
            [TimeControl "300+5"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 1-0
            """.trimIndent()
        ).single()

        assertEquals("Ana", record.playerName)
        assertEquals("Luis", record.opponent)
        assertEquals("5 + 5", record.timeLabel)
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6", "e1g1", "f8e7"), record.uciMoves)
        assertEquals(10, record.movesSan.size)
        val replay = ChessEngine()
        record.uciMoves.forEach { uci ->
            val from = com.chessx.core.Move.algebraicToSquare(uci.substring(0, 2))!!
            val to = com.chessx.core.Move.algebraicToSquare(uci.substring(2, 4))!!
            val promotion = when (uci.lastOrNull()) {
                'q' -> com.chessx.core.PieceType.QUEEN
                'r' -> com.chessx.core.PieceType.ROOK
                'b' -> com.chessx.core.PieceType.BISHOP
                'n' -> com.chessx.core.PieceType.KNIGHT
                else -> null
            }
            assertTrue(replay.doMove(replay.findMove(from, to, promotion)!!))
        }
        assertEquals(record.finalFen, replay.toFEN())
    }

    @Test
    fun ignoresCommentsVariationsAndMoveAnnotations() {
        val record = PgnArchive.parse(
            """
            [Event "Casual"]
            [White "Ana"]
            [Black "Luis"]
            [Result "1/2-1/2"]

            1. e4 {opening} e5 (1... c5 2. Nf3) 2. Nf3 $1 Nc6 1/2-1/2
            """.trimIndent()
        ).single()

        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6"), record.uciMoves)
        assertEquals("1/2-1/2", record.result)
    }

    @Test
    fun parsesStandardSanDisambiguation() {
        val record = PgnArchive.parse(
            """
            [Event "Disambiguation"]
            [White "Ana"]
            [Black "Luis"]
            [Result "1-0"]

            1. d4 d5 2. Nf3 Nf6 3. Nbd2 e6 1-0
            """.trimIndent()
        ).single()

        assertEquals("b1d2", record.uciMoves[4])
    }

    @Test
    fun importsMultipleGamesAndRoundTripsChessXUci() {
        val firstGame = """
            [Event "One"]
            [White "Ana"]
            [Black "Luis"]
            [Result "1-0"]

            1. e4 e5 1-0
        """.trimIndent()
        val secondGame = """
            [Event "Two"]
            [White "Eva"]
            [Black "Noe"]
            [Result "0-1"]

            1. d4 d5 0-1
        """.trimIndent()

        val records = PgnArchive.parse("$firstGame\n\n$secondGame")
        val exported = records.joinToString("\n\n") { record ->
            buildPgn(record, record.playerName, record.opponent)
        }
        val reimported = PgnArchive.parse(exported)

        assertEquals(2, reimported.size)
        assertEquals(records.map { it.uciMoves }, reimported.map { it.uciMoves })
        assertTrue(reimported.all { it.pgn.contains("[CHESSXUCI ") })
    }

    @Test
    fun rejectsIllegalMovesAndUnfinishedGames() {
        val illegal = """
            [Event "Invalid"]
            [Result "1-0"]

            1. e5 1-0
        """.trimIndent()
        val unfinished = """
            [Event "Unfinished"]
            [Result "*"]

            1. e4 *
        """.trimIndent()

        assertTrue(runCatching { PgnArchive.parse(illegal) }.isFailure)
        assertTrue(runCatching { PgnArchive.parse(unfinished) }.isFailure)
    }

    @Test
    fun historyKeepsOnlyTheFiftyNewestGames() {
        val games = (0L until 60L).map { date ->
            GameRecord(
                dateMs = date,
                opponent = "Rival $date",
                result = "1-0",
                localWon = true,
                localColorWhite = true,
                timeLabel = "5 + 0",
                movesSan = emptyList(),
                pgn = "",
                finalFen = ""
            )
        }

        val retained = GameHistoryStore.keepLatestRecords(games)

        assertEquals(50, retained.size)
        assertEquals(59L, retained.first().dateMs)
        assertEquals(10L, retained.last().dateMs)
    }
}
