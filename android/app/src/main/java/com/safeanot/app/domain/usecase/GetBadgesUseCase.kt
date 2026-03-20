/**
 * Use case for observing all badges with their unlock state.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBadgesUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
) {

    operator fun invoke(): Flow<List<BadgeProgress>> {
        return badgeRepository.observeAllBadges()
    }
}
