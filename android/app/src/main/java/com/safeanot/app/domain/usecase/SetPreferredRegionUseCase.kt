/**
 * Use case for persisting the user's preferred alert region.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetPreferredRegionUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(region: AlertRegionFilter) {
        repository.setRegion(region)
    }
}
