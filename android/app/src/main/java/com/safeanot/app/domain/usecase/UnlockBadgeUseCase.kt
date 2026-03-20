/**
 * Thin use case wrapper for unlocking a badge.
 * Ensures ViewModels never inject BadgeRepository directly,
 * following the project's clean architecture convention.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.repository.BadgeRepository
import javax.inject.Inject

open class UnlockBadgeUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
) {

    /** Returns true if newly unlocked, false if already unlocked. */
    open suspend operator fun invoke(type: BadgeType): Boolean {
        return badgeRepository.unlockBadge(type)
    }
}
