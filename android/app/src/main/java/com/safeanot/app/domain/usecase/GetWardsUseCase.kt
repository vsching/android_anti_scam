/**
 * Use case for observing wards (devices this guardian is monitoring).
 * Uses flow{} builder to bridge the suspend getDeviceId() call with the Flow from the repository.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.repository.GuardianRepository
import com.safeanot.app.util.DeviceIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetWardsUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val deviceIdProvider: DeviceIdProvider,
) {
    /**
     * Observe all wards for the current device.
     * The flow{} builder provides suspend context for getOrCreateDeviceId().
     */
    operator fun invoke(): Flow<List<GuardianPairing>> = flow {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        emitAll(guardianRepository.getWards(deviceId))
    }
}
