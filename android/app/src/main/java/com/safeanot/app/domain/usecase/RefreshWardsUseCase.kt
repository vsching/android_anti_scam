/**
 * Use case for refreshing ward data from the backend.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.repository.GuardianRepository
import com.safeanot.app.util.DeviceIdProvider
import javax.inject.Inject

class RefreshWardsUseCase @Inject constructor(
    private val guardianRepository: GuardianRepository,
    private val deviceIdProvider: DeviceIdProvider,
) {
    /**
     * Refresh ward data from the backend API.
     */
    suspend operator fun invoke() {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        guardianRepository.refreshWards(deviceId)
    }
}
