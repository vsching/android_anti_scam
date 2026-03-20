/**
 * Use case for claiming a guardian pairing code to establish a relationship.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.repository.GuardianRepository
import javax.inject.Inject

class ClaimPairingCodeUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
) {
    /**
     * Claim a pairing code.
     * @param code The pairing code to claim.
     * @param label Human-readable label for this device.
     * @return The created pairing relationship.
     */
    suspend operator fun invoke(code: String, label: String): GuardianPairing {
        return guardianRepository.claimPairingCode(code, label)
    }
}
