/**
 * Use case for loading a single alert by ID from the repository cache.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import javax.inject.Inject

class GetAlertByIdUseCase @Inject constructor(
    private val repository: AlertsRepository,
) {
    suspend operator fun invoke(alertId: String): ScamAlert? =
        repository.getAlertById(alertId)
}
