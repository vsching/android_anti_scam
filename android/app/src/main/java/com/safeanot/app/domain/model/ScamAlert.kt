/**
 * Domain model for a scam alert.
 */
package com.safeanot.app.domain.model

/**
 * Severity levels for scam alerts.
 */
enum class AlertSeverity {
    HIGH, MEDIUM, LOW;

    companion object {
        fun fromString(value: String): AlertSeverity =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LOW
    }
}

/**
 * Region a scam alert applies to.
 */
enum class AlertRegion {
    MY, SG, BOTH;

    companion object {
        fun fromString(value: String): AlertRegion =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BOTH
    }
}

data class ScamAlert(
    val id: String,
    val title: String,
    val description: String,
    val scamType: String,
    val severity: AlertSeverity,
    val region: AlertRegion,
    val reportCount: Int,
    val createdAt: String,
    val createdAtEpochMs: Long,
)
