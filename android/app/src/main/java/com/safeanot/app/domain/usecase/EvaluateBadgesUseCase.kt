/**
 * Placeholder use case for badge evaluation.
 * Returns an empty list for now -- E10-002 fills in the real evaluation logic.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeType
import javax.inject.Inject

open class EvaluateBadgesUseCase @Inject constructor() {

    @Suppress("RedundantSuspendModifier")
    open suspend operator fun invoke(): List<BadgeType> {
        // Placeholder: E10-002 will inject repositories and evaluate badge conditions.
        return emptyList()
    }
}
