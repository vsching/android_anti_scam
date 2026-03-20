package com.safeanot.app.testutil

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.repository.GuardianRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeGuardianRepository : GuardianRepository {

    val pairingsFlow = MutableStateFlow<List<GuardianPairing>>(emptyList())
    var generateCodeResult: PairingCode = PairingCode("TEST-CODE", Long.MAX_VALUE)
    var claimCodeResult: GuardianPairing = GuardianPairing(
        id = "test-id",
        deviceId = "device-1",
        pairedDeviceId = "device-2",
        role = GuardianRole.GUARDIAN,
        label = "Test",
        createdAt = System.currentTimeMillis(),
    )
    var shouldThrow: Boolean = false
    var deletedPairingIds = mutableListOf<String>()
    var refreshCalled = false

    override suspend fun generatePairingCode(role: String, label: String): PairingCode {
        if (shouldThrow) throw RuntimeException("Test error")
        return generateCodeResult
    }

    override suspend fun claimPairingCode(code: String, label: String): GuardianPairing {
        if (shouldThrow) throw RuntimeException("Test error")
        val pairing = claimCodeResult
        pairingsFlow.value = pairingsFlow.value + pairing
        return pairing
    }

    override fun getAllPairings(): Flow<List<GuardianPairing>> = pairingsFlow

    override suspend fun deletePairing(pairingId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        deletedPairingIds.add(pairingId)
        pairingsFlow.value = pairingsFlow.value.filter { it.id != pairingId }
    }

    override suspend fun refreshPairings() {
        if (shouldThrow) throw RuntimeException("Test error")
        refreshCalled = true
    }

    override fun getGuardianCount(): Flow<Int> = pairingsFlow.map { it.size }

    override suspend fun getDeviceId(): String = "test-device-id"
}
