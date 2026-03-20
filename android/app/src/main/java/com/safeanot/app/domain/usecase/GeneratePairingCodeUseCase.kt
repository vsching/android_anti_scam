/**
 * Use case for generating a guardian pairing code.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.repository.GuardianRepository
import javax.inject.Inject

class GeneratePairingCodeUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
) {
    /**
     * Generate a pairing code for the current device.
     * @param role The role the code generator wants ("GUARDIAN" or "WARD").
     * @param label Human-readable label for this device.
     */
    suspend operator fun invoke(role: String, label: String): PairingCode {
        return guardianRepository.generatePairingCode(role, label)
    }
}
