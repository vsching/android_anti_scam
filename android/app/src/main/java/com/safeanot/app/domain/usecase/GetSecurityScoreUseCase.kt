/**
 * Use case that observes the current security score as a Flow.
 * Score formula: (securedItems / totalDetectedItems) * 100.
 * Score band thresholds: RED (0-49), AMBER (50-79), GREEN (80-100).
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSecurityScoreUseCase @Inject constructor(
    private val repository: AuditRepository,
) {
    operator fun invoke(): Flow<SecurityScore> {
        return repository.getSecurityScore().map { score ->
            score ?: SecurityScore()
        }
    }
}
