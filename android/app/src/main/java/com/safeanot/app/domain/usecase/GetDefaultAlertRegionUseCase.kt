/**
 * Use case for detecting the default alert region filter from device locale.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.AlertsRepository
import javax.inject.Inject

class GetDefaultAlertRegionUseCase @Inject constructor(
    private val repository: AlertsRepository,
) {
    operator fun invoke(): AlertRegionFilter = repository.getDefaultRegionFilter()
}
