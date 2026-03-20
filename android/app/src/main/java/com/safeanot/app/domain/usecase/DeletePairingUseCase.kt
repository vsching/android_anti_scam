/**
 * Use case for deleting a guardian pairing relationship.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.repository.GuardianRepository
import javax.inject.Inject

class DeletePairingUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
) {
    /**
     * Delete a pairing by its ID.
     */
    suspend operator fun invoke(pairingId: String) {
        guardianRepository.deletePairing(pairingId)
    }
}
