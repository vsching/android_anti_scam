/**
 * Use case for observing the featured "Scam of the Week" alert.
 * Returns the first high-severity alert matching the user's preferred region,
 * or null if none exist.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetFeaturedAlertUseCase @Inject constructor(
    private val alertsRepository: AlertsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<ScamAlert?> {
        return combine(
            userPreferencesRepository.getRegion(),
            alertsRepository.observeAlerts(AlertRegionFilter.ALL),
        ) { region, alerts ->
            val regionFiltered = filterByRegion(alerts, region)
            regionFiltered.firstOrNull { it.severity == AlertSeverity.HIGH }
        }
    }

    private fun filterByRegion(
        alerts: List<ScamAlert>,
        region: AlertRegionFilter,
    ): List<ScamAlert> {
        if (region == AlertRegionFilter.ALL) return alerts
        val apiRegion = region.toApiRegion() ?: return alerts
        return alerts.filter {
            it.region.name.equals(apiRegion, ignoreCase = true) ||
                it.region.name.equals("BOTH", ignoreCase = true)
        }
    }
}
