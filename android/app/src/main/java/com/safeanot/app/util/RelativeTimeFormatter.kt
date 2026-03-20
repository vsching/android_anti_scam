/**
 * Utility for formatting timestamps into human-readable relative time strings.
 * Pure function with no Context dependency.
 */
package com.safeanot.app.util

object RelativeTimeFormatter {

    private const val SECOND_MS = 1000L
    private const val MINUTE_MS = 60 * SECOND_MS
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS
    private const val WEEK_MS = 7 * DAY_MS

    /**
     * Format a timestamp (in milliseconds) as a relative time string.
     * @param timestampMs The timestamp to format, in epoch milliseconds.
     * @param nowMs The current time in epoch milliseconds (default: System.currentTimeMillis()).
     * @return A human-readable relative time string like "Just now", "5 min ago", "2 hours ago".
     */
    fun format(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diffMs = nowMs - timestampMs
        if (diffMs < 0) return "Just now"

        return when {
            diffMs < MINUTE_MS -> "Just now"
            diffMs < HOUR_MS -> {
                val minutes = (diffMs / MINUTE_MS).toInt()
                if (minutes == 1) "1 min ago" else "$minutes min ago"
            }
            diffMs < DAY_MS -> {
                val hours = (diffMs / HOUR_MS).toInt()
                if (hours == 1) "1 hour ago" else "$hours hours ago"
            }
            diffMs < WEEK_MS -> {
                val days = (diffMs / DAY_MS).toInt()
                if (days == 1) "1 day ago" else "$days days ago"
            }
            else -> {
                val weeks = (diffMs / WEEK_MS).toInt()
                if (weeks == 1) "1 week ago" else "$weeks weeks ago"
            }
        }
    }
}
