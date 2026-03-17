package com.safeanot.app.feature.alerts

import com.safeanot.app.domain.model.AlertRegion
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertShareFormatterTest {

    private val testAlert = ScamAlert(
        id = "test-123",
        title = "Fake Banking APK",
        description = "Scammers sending malware disguised as banking apps.",
        scamType = "malware",
        severity = AlertSeverity.HIGH,
        region = AlertRegion.MY,
        reportCount = 42,
        createdAt = "2026-03-15T10:00:00",
        createdAtEpochMs = 0L,
    )

    @Test
    fun `formatted text contains alert title`() {
        val result = AlertShareFormatter.format(testAlert)
        assertTrue(result.contains("Fake Banking APK"))
    }

    @Test
    fun `formatted text contains severity`() {
        val result = AlertShareFormatter.format(testAlert)
        assertTrue(result.contains("HIGH"))
    }

    @Test
    fun `formatted text contains region`() {
        val result = AlertShareFormatter.format(testAlert)
        assertTrue(result.contains("MY"))
    }

    @Test
    fun `formatted text contains report count`() {
        val result = AlertShareFormatter.format(testAlert)
        assertTrue(result.contains("42"))
    }

    @Test
    fun `formatted text contains deep link`() {
        val result = AlertShareFormatter.format(testAlert)
        assertTrue(result.contains("https://safeanot.com/alert/test-123"))
    }

    @Test
    fun `formatted text contains description`() {
        val result = AlertShareFormatter.format(testAlert)
        assertTrue(result.contains("Scammers sending malware"))
    }
}
