/**
 * Use case for tracking share event analytics.
 * Delegates to ShareEventRepository with rate limiting and offline support.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.repository.ShareEventRepository
import javax.inject.Inject

class TrackShareEventUseCase @Inject constructor(
    private val shareEventRepository: ShareEventRepository,
) {
    /**
     * Track a share event.
     * @return true if recorded, false if rate-limited.
     */
    suspend operator fun invoke(event: ShareEventModel): Boolean {
        return shareEventRepository.trackEvent(event)
    }
}
