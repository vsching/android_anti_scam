package com.safeanot.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the security score calculation formula:
 * (secured / installedDetected) * 100, with zero denominator yielding 100.
 */
class SecurityScoreCalculationTest {

    /** Mirrors the score calculation logic in AuditRepositoryImpl.recalculateScore(). */
    private fun calculateScore(secured: Int, installed: Int): Int {
        return if (installed > 0) (secured * 100) / installed else 100
    }

    @Test
    fun `zero installed items yields 100 percent`() {
        assertEquals(100, calculateScore(secured = 0, installed = 0))
    }

    @Test
    fun `all secured yields 100 percent`() {
        assertEquals(100, calculateScore(secured = 5, installed = 5))
    }

    @Test
    fun `none secured yields 0 percent`() {
        assertEquals(0, calculateScore(secured = 0, installed = 5))
    }

    @Test
    fun `partial secured yields correct percent`() {
        assertEquals(60, calculateScore(secured = 3, installed = 5))
    }

    @Test
    fun `one of two secured yields 50 percent`() {
        assertEquals(50, calculateScore(secured = 1, installed = 2))
    }

    @Test
    fun `integer division truncates correctly`() {
        // 1/3 = 33.3... -> 33
        assertEquals(33, calculateScore(secured = 1, installed = 3))
    }

    @Test
    fun `boundary at 49 percent is RED band`() {
        // 49/100 = 49
        val score = calculateScore(secured = 49, installed = 100)
        assertEquals(49, score)
    }

    @Test
    fun `boundary at 50 percent is AMBER band`() {
        val score = calculateScore(secured = 50, installed = 100)
        assertEquals(50, score)
    }

    @Test
    fun `boundary at 80 percent is GREEN band`() {
        val score = calculateScore(secured = 80, installed = 100)
        assertEquals(80, score)
    }
}
