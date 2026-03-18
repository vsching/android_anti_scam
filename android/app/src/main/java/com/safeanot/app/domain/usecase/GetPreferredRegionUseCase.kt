/**
 * Use case for observing the user's preferred alert region.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPreferredRegionUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<AlertRegionFilter> = repository.getRegion()
}
