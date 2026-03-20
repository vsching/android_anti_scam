package com.safeanot.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeFormatterTest {

    private val now = 1_000_000_000_000L // Fixed reference time

    @Test
    fun `just now for less than 60 seconds`() {
        assertEquals("Just now", RelativeTimeFormatter.format(now - 30_000L, now))
        assertEquals("Just now", RelativeTimeFormatter.format(now - 59_999L, now))
        assertEquals("Just now", RelativeTimeFormatter.format(now, now))
    }

    @Test
    fun `minutes ago for 1-59 minutes`() {
        assertEquals("1 min ago", RelativeTimeFormatter.format(now - 60_000L, now))
        assertEquals("5 min ago", RelativeTimeFormatter.format(now - 5 * 60_000L, now))
        assertEquals("59 min ago", RelativeTimeFormatter.format(now - 59 * 60_000L, now))
    }

    @Test
    fun `hours ago for 1-23 hours`() {
        assertEquals("1 hour ago", RelativeTimeFormatter.format(now - 3_600_000L, now))
        assertEquals("2 hours ago", RelativeTimeFormatter.format(now - 2 * 3_600_000L, now))
        assertEquals("23 hours ago", RelativeTimeFormatter.format(now - 23 * 3_600_000L, now))
    }

    @Test
    fun `days ago for 1-6 days`() {
        assertEquals("1 day ago", RelativeTimeFormatter.format(now - 86_400_000L, now))
        assertEquals("3 days ago", RelativeTimeFormatter.format(now - 3 * 86_400_000L, now))
        assertEquals("6 days ago", RelativeTimeFormatter.format(now - 6 * 86_400_000L, now))
    }

    @Test
    fun `weeks ago for 7+ days`() {
        assertEquals("1 week ago", RelativeTimeFormatter.format(now - 7 * 86_400_000L, now))
        assertEquals("2 weeks ago", RelativeTimeFormatter.format(now - 14 * 86_400_000L, now))
        assertEquals("4 weeks ago", RelativeTimeFormatter.format(now - 30 * 86_400_000L, now))
    }

    @Test
    fun `just now for future timestamps`() {
        assertEquals("Just now", RelativeTimeFormatter.format(now + 60_000L, now))
    }
}
