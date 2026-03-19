package com.safeanot.app.util

import android.graphics.Bitmap
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.domain.model.SecurityScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityScoreCardGeneratorTest {

    @Test
    fun `generateSquare returns 1080x1080 bitmap`() {
        val score = SecurityScore(totalItems = 10, securedItems = 8, scorePercent = 80)
        val bitmap = SecurityScoreCardGenerator.generateSquare(score)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
    }

    @Test
    fun `generateVertical returns 1080x1920 bitmap`() {
        val score = SecurityScore(totalItems = 10, securedItems = 8, scorePercent = 80)
        val bitmap = SecurityScoreCardGenerator.generateVertical(score)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }

    @Test
    fun `generateSquare returns non-null for RED band`() {
        val score = SecurityScore(totalItems = 10, securedItems = 3, scorePercent = 30)
        assertEquals(ScoreBand.RED, score.band)
        val bitmap = SecurityScoreCardGenerator.generateSquare(score)
        assertNotNull(bitmap)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `generateSquare returns non-null for AMBER band`() {
        val score = SecurityScore(totalItems = 10, securedItems = 6, scorePercent = 60)
        assertEquals(ScoreBand.AMBER, score.band)
        val bitmap = SecurityScoreCardGenerator.generateSquare(score)
        assertNotNull(bitmap)
    }

    @Test
    fun `generateSquare returns non-null for GREEN band`() {
        val score = SecurityScore(totalItems = 10, securedItems = 9, scorePercent = 90)
        assertEquals(ScoreBand.GREEN, score.band)
        val bitmap = SecurityScoreCardGenerator.generateSquare(score)
        assertNotNull(bitmap)
    }

    @Test
    fun `generateVertical returns non-null for all bands`() {
        val scores = listOf(
            SecurityScore(scorePercent = 20),  // RED
            SecurityScore(scorePercent = 65),  // AMBER
            SecurityScore(scorePercent = 95),  // GREEN
        )
        for (score in scores) {
            val bitmap = SecurityScoreCardGenerator.generateVertical(score)
            assertNotNull(bitmap)
        }
    }

    @Test
    fun `generateSquare with default score produces valid bitmap`() {
        val bitmap = SecurityScoreCardGenerator.generateSquare(SecurityScore())
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
    }
}
