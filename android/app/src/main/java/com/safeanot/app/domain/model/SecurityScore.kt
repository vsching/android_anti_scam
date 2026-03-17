/**
 * Domain model representing the user's overall security score.
 */
package com.safeanot.app.domain.model

/**
 * Score band thresholds for UI color coding.
 */
enum class ScoreBand {
    /** Score 0-49: high risk. */
    RED,

    /** Score 50-79: moderate risk. */
    AMBER,

    /** Score 80-100: low risk. */
    GREEN;

    companion object {
        fun fromPercent(percent: Int): ScoreBand = when {
            percent >= 80 -> GREEN
            percent >= 50 -> AMBER
            else -> RED
        }
    }
}

data class SecurityScore(
    val totalItems: Int = 0,
    val securedItems: Int = 0,
    val scorePercent: Int = 0,
    val lastAuditDate: Long = 0L,
) {
    /** The score band derived from the percentage. */
    val band: ScoreBand
        get() = ScoreBand.fromPercent(scorePercent)
}
