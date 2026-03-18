/**
 * Use case that observes audit statistics for the Profile screen.
 * Combines audit count, last audit timestamp, and security score into a single Flow.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class AuditStats(
    val totalAudits: Int = 0,
    val lastAuditTimestamp: Long? = null,
    val securityScore: Int = 0,
)

class GetAuditStatsUseCase @Inject constructor(
    private val repository: AuditRepository,
) {
    operator fun invoke(): Flow<AuditStats> {
        return combine(
            repository.getCompletedAuditCount(),
            repository.getLastAuditTimestamp(),
            repository.getSecurityScore(),
        ) { count, timestamp, score ->
            AuditStats(
                totalAudits = count,
                lastAuditTimestamp = timestamp,
                securityScore = score?.scorePercent ?: 0,
            )
        }
    }
}
