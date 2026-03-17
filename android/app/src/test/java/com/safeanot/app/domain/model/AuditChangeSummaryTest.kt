package com.safeanot.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditChangeSummaryTest {

    @Test
    fun `hasChanges is false when no changes`() {
        val summary = AuditChangeSummary()
        assertFalse(summary.hasChanges)
    }

    @Test
    fun `hasChanges is true with newly risky apps`() {
        val summary = AuditChangeSummary(newlyRiskyApps = listOf("WhatsApp"))
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `hasChanges is true with regressions`() {
        val summary = AuditChangeSummary(regressions = listOf("Chrome"))
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `notification text for no changes`() {
        val summary = AuditChangeSummary()
        assertEquals("No changes detected.", summary.toNotificationText())
    }

    @Test
    fun `notification text for newly risky apps`() {
        val summary = AuditChangeSummary(newlyRiskyApps = listOf("WhatsApp", "Telegram"))
        assertEquals("2 new risky app(s) detected.", summary.toNotificationText())
    }

    @Test
    fun `notification text for regressions`() {
        val summary = AuditChangeSummary(regressions = listOf("Chrome"))
        assertEquals("1 app(s) need re-securing.", summary.toNotificationText())
    }

    @Test
    fun `notification text for both changes`() {
        val summary = AuditChangeSummary(
            newlyRiskyApps = listOf("WhatsApp"),
            regressions = listOf("Chrome"),
        )
        assertEquals(
            "1 new risky app(s) detected. 1 app(s) need re-securing.",
            summary.toNotificationText(),
        )
    }
}
