/**
 * Concrete implementation of GuardianRepository.
 * Offline-first: persists pairings in Room and syncs with the backend API.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.GuardianDao
import com.safeanot.app.data.local.entity.GuardianPairingEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.ClaimPairingCodeRequest
import com.safeanot.app.data.remote.model.DeletePairingRequest
import com.safeanot.app.data.remote.model.GeneratePairingCodeRequest
import com.safeanot.app.data.remote.model.HeartbeatRequest
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.repository.GuardianRepository
import com.safeanot.app.util.DeviceIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GuardianRepositoryImpl @Inject constructor(
    private val guardianDao: GuardianDao,
    private val api: SafeAnotApi,
    private val deviceIdProvider: DeviceIdProvider,
) : GuardianRepository {

    override suspend fun generatePairingCode(role: String, label: String): PairingCode {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        val request = GeneratePairingCodeRequest(
            deviceId = deviceId,
            role = role,
            label = label,
        )
        val response = api.generatePairingCode(request)
        return response.toDomain()
    }

    override suspend fun claimPairingCode(code: String, label: String): GuardianPairing {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        val request = ClaimPairingCodeRequest(
            deviceId = deviceId,
            code = code,
            label = label,
        )
        val dto = api.claimPairingCode(request)
        val pairing = dto.toDomain()
        guardianDao.insertAll(listOf(GuardianPairingEntity.fromDomain(pairing)))
        return pairing
    }

    override fun getAllPairings(): Flow<List<GuardianPairing>> {
        return guardianDao.getAllPairings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deletePairing(pairingId: String) {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        val request = DeletePairingRequest(
            deviceId = deviceId,
            pairingId = pairingId,
        )
        api.deletePairing(request)
        guardianDao.deleteById(pairingId)
    }

    override suspend fun refreshPairings() {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        val wards = api.getWards(deviceId)
        val guardians = api.getGuardians(deviceId)
        val allPairings = (wards + guardians).map { dto ->
            GuardianPairingEntity.fromDomain(dto.toDomain())
        }
        guardianDao.deleteAll()
        guardianDao.insertAll(allPairings)
    }

    override fun getGuardianCount(): Flow<Int> {
        return guardianDao.getGuardianCount()
    }

    override suspend fun getDeviceId(): String {
        return deviceIdProvider.getOrCreateDeviceId()
    }

    override suspend fun sendHeartbeat(
        securityScore: Int,
        securedItems: Int,
        totalItems: Int,
        playProtectEnabled: Boolean,
    ) {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        val request = HeartbeatRequest(
            deviceId = deviceId,
            securityScore = securityScore,
            securedItems = securedItems,
            totalItems = totalItems,
            playProtectEnabled = playProtectEnabled,
            timestamp = System.currentTimeMillis() / 1000,
        )
        api.postHeartbeat(request)
    }
}
