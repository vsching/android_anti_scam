/**
 * Use case that triggers a full device audit, checking which tracked packages are installed
 * and updating their audit status accordingly.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.repository.AuditRepository
import javax.inject.Inject

class RunAuditUseCase @Inject constructor(
    private val repository: AuditRepository,
) {
    suspend operator fun invoke() {
        repository.runAudit()
    }
}
