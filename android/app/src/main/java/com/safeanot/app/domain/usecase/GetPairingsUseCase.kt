/**
 * Use case for observing all guardian pairings.
 * Uses flow{} builder to bridge the suspend getDeviceId() call with the Flow from the repository.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.repository.GuardianRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetPairingsUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
) {
    /**
     * Observe all pairings. Attempts a background refresh before emitting cached data.
     */
    operator fun invoke(): Flow<List<GuardianPairing>> = flow {
        // Attempt to refresh from backend; proceed with cached data on failure
        try {
            guardianRepository.refreshPairings()
        } catch (_: Exception) {
            // Offline — use cached data
        }
        emitAll(guardianRepository.getAllPairings())
    }
}
