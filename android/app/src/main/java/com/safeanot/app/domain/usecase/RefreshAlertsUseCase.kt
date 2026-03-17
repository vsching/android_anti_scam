/**
 * Use case for refreshing scam alerts from the API.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.AlertsRepository
import javax.inject.Inject

class RefreshAlertsUseCase @Inject constructor(
    private val repository: AlertsRepository,
) {
    suspend operator fun invoke(filter: AlertRegionFilter) {
        repository.refreshAlerts(filter)
    }
}
