package com.chessx.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StockfishEngineManagerTest {
    @Test
    fun percentStrengthUsesStockfishUciLimitsWithoutInventedElo() {
        assertEquals(StockfishStrength(1320, 0), StockfishEngineManager.strengthForPercent(0))
        assertEquals(StockfishStrength(2255, 10), StockfishEngineManager.strengthForPercent(50))
        assertEquals(StockfishStrength(3190, 20), StockfishEngineManager.strengthForPercent(100))
        assertEquals(StockfishEngineManager.strengthForPercent(0), StockfishEngineManager.strengthForPercent(-1))
        assertEquals(StockfishEngineManager.strengthForPercent(100), StockfishEngineManager.strengthForPercent(101))
    }
}
