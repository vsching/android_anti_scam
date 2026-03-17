package com.safeanot.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityScoreTest {

    @Test
    fun `score band RED for 0 percent`() {
        val score = SecurityScore(scorePercent = 0)
        assertEquals(ScoreBand.RED, score.band)
    }

    @Test
    fun `score band RED for 49 percent`() {
        val score = SecurityScore(scorePercent = 49)
        assertEquals(ScoreBand.RED, score.band)
    }

    @Test
    fun `score band AMBER for 50 percent`() {
        val score = SecurityScore(scorePercent = 50)
        assertEquals(ScoreBand.AMBER, score.band)
    }

    @Test
    fun `score band AMBER for 79 percent`() {
        val score = SecurityScore(scorePercent = 79)
        assertEquals(ScoreBand.AMBER, score.band)
    }

    @Test
    fun `score band GREEN for 80 percent`() {
        val score = SecurityScore(scorePercent = 80)
        assertEquals(ScoreBand.GREEN, score.band)
    }

    @Test
    fun `score band GREEN for 100 percent`() {
        val score = SecurityScore(scorePercent = 100)
        assertEquals(ScoreBand.GREEN, score.band)
    }

    @Test
    fun `default score has zero values`() {
        val score = SecurityScore()
        assertEquals(0, score.totalItems)
        assertEquals(0, score.securedItems)
        assertEquals(0, score.scorePercent)
        assertEquals(0L, score.lastAuditDate)
    }
}
