/**
 * Domain model representing the user's overall security score.
 */
package com.safeanot.app.domain.model

data class SecurityScore(
    val totalItems: Int = 0,
    val securedItems: Int = 0,
    val scorePercent: Int = 0,
    val lastAuditDate: Long = 0L,
)
