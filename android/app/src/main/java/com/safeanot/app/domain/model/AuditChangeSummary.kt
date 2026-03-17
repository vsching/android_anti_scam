/**
 * Domain model representing changes detected between two consecutive audit scans.
 * Used by the reminder worker to decide whether to notify the user.
 */
package com.safeanot.app.domain.model

data class AuditChangeSummary(
    /** Apps that are newly installed and pose a sideloading risk. */
    val newlyRiskyApps: List<String> = emptyList(),

    /** Apps that were previously secured but have regressed to needing review. */
    val regressions: List<String> = emptyList(),
) {
    /** Whether any actionable changes were detected. */
    val hasChanges: Boolean
        get() = newlyRiskyApps.isNotEmpty() || regressions.isNotEmpty()

    /** Human-readable summary of the changes for notification text. */
    fun toNotificationText(): String {
        val parts = mutableListOf<String>()
        if (newlyRiskyApps.isNotEmpty()) {
            parts.add("${newlyRiskyApps.size} new risky app(s) detected")
        }
        if (regressions.isNotEmpty()) {
            parts.add("${regressions.size} app(s) need re-securing")
        }
        return if (parts.isEmpty()) "No changes detected." else parts.joinToString(". ") + "."
    }
}
