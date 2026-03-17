/**
 * Use case for observing the scam alerts feed by region filter.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAlertsUseCase @Inject constructor(
    private val repository: AlertsRepository,
) {
    operator fun invoke(filter: AlertRegionFilter): Flow<List<ScamAlert>> =
        repository.observeAlerts(filter)
}
