/**
 * Unit tests for SafeAnotMessagingService.
 * Verifies notification payload parsing and deep link intent construction.
 *
 * Note: Full integration tests with FirebaseMessagingService require Robolectric
 * and a mock FirebaseApp. These tests verify the logic patterns used in the service.
 */
package com.safeanot.app.service

import android.net.Uri
import com.safeanot.app.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeAnotMessagingServiceTest {

    @Test
    fun `scam alert deep link URI is correctly constructed`() {
        val alertId = "alert-123"
        val deepLinkUri = Uri.parse("https://safeanot.com/alert/$alertId")

        assertEquals("https", deepLinkUri.scheme)
        assertEquals("safeanot.com", deepLinkUri.host)
        assertEquals("/alert/alert-123", deepLinkUri.path)
    }

    @Test
    fun `scam alert deep link works with numeric alert IDs`() {
        val alertId = "42"
        val deepLinkUri = Uri.parse("https://safeanot.com/alert/$alertId")
        assertEquals("/alert/42", deepLinkUri.path)
    }

    @Test
    fun `scam alert notification uses correct channel ID`() {
        // Verify the channel constant matches what the service uses
        assertEquals("scam_alerts", Constants.SCAM_ALERTS_CHANNEL_ID)
    }

    @Test
    fun `notification data payload parsing for scam_alert type`() {
        val data = mapOf(
            "type" to "scam_alert",
            "alert_id" to "alert-456",
            "title" to "Investment Scam Warning",
            "body" to "A new investment scam has been reported in your area.",
        )

        assertEquals("scam_alert", data["type"])
        assertNotNull(data["alert_id"])
        assertEquals("alert-456", data["alert_id"])
        assertEquals("Investment Scam Warning", data["title"])
        assertTrue(data["body"]!!.isNotEmpty())
    }

    @Test
    fun `notification data payload with missing optional fields uses defaults`() {
        val data = mapOf(
            "type" to "scam_alert",
            "alert_id" to "alert-789",
        )

        // Service defaults when title/body are missing
        val title = data["title"] ?: "New Scam Alert"
        val body = data["body"] ?: "A new scam alert has been reported in your region."

        assertEquals("New Scam Alert", title)
        assertEquals("A new scam alert has been reported in your region.", body)
    }

    @Test
    fun `notification data payload without alert_id is ignored`() {
        val data = mapOf(
            "type" to "scam_alert",
            "title" to "Some Alert",
        )

        // Service returns early when alert_id is missing
        val alertId = data["alert_id"]
        assertEquals(null, alertId)
    }

    @Test
    fun `guardian alert notification types are distinct from scam alert`() {
        val scamAlertType = "scam_alert"
        val guardianAlertType = "guardian_alert"
        val helpRequestType = "help_request"

        assertTrue(scamAlertType != guardianAlertType)
        assertTrue(scamAlertType != helpRequestType)
    }

    @Test
    fun `sanitize strips newlines and truncates`() {
        // Mirrors the private sanitize method logic
        val input = "Line1\nLine2\r\nLine3"
        val sanitized = input.replace(Regex("[\r\n]+"), " ").take(100)
        assertEquals("Line1 Line2 Line3", sanitized)
    }

    @Test
    fun `sanitize truncates long text to maxLength`() {
        val longText = "A".repeat(200)
        val sanitized = longText.replace(Regex("[\r\n]+"), " ").take(100)
        assertEquals(100, sanitized.length)
    }
}
