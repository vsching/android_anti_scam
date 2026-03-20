/**
 * Use case for sending a "Help Me Fix This" request to all guardians.
 * Gathers the current security score and unfixed items, then sends the request.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.GuardianRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SendHelpRequestUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val auditRepository: AuditRepository,
) {
    /**
     * Sends a help request with the current security score and unfixed items.
     * @throws Exception if the request fails (e.g., rate limited, no guardians).
     */
    suspend operator fun invoke() {
        val auditItems = auditRepository.getAllAuditItems().first()
        val unfixedItems = auditItems
            .filter { it.status == AuditStatus.NEEDS_REVIEW }
            .map { it.appName }

        val totalItems = auditItems.size
        val securedItems = auditItems.count { it.status == AuditStatus.SECURED || it.status == AuditStatus.NOT_INSTALLED }
        val scorePercent = if (totalItems > 0) (securedItems * 100) / totalItems else 0

        guardianRepository.sendHelpRequest(scorePercent, unfixedItems)
    }
}
