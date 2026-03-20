package com.safeanot.app.testutil

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.model.HeartbeatEntry
import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.model.WardHeartbeatHistory
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
    var refreshWardsCalled = false
    var heartbeatSent = false
    var lastHeartbeatScore: Int? = null
    var helpRequestSent = false
    var lastHelpRequestScore: Int? = null
    var lastHelpRequestUnfixedItems: List<String>? = null
    var wardHeartbeatHistory: WardHeartbeatHistory = WardHeartbeatHistory(
        deviceId = "ward-device",
        displayName = "Ward Device",
        heartbeats = emptyList(),
    )

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

    override fun getWardRoleCount(): Flow<Int> = pairingsFlow.map { pairings ->
        pairings.count { it.role == GuardianRole.WARD }
    }

    override suspend fun getDeviceId(): String = "test-device-id"

    override suspend fun sendHeartbeat(
        securityScore: Int,
        securedItems: Int,
        totalItems: Int,
        playProtectEnabled: Boolean,
    ) {
        if (shouldThrow) throw RuntimeException("Test error")
        heartbeatSent = true
        lastHeartbeatScore = securityScore
    }

    override suspend fun sendHelpRequest(securityScore: Int, unfixedItems: List<String>) {
        if (shouldThrow) throw RuntimeException("Test error")
        helpRequestSent = true
        lastHelpRequestScore = securityScore
        lastHelpRequestUnfixedItems = unfixedItems
    }

    override fun getWards(deviceId: String): Flow<List<GuardianPairing>> {
        return pairingsFlow.map { pairings ->
            pairings.filter { it.role == GuardianRole.GUARDIAN }
        }
    }

    override suspend fun refreshWards(deviceId: String) {
        if (shouldThrow) throw RuntimeException("Test error")
        refreshWardsCalled = true
    }

    override suspend fun getWardHeartbeatHistory(wardDeviceId: String, days: Int): WardHeartbeatHistory {
        if (shouldThrow) throw RuntimeException("Test error")
        return wardHeartbeatHistory
    }

    override suspend fun getPairingIdByPairedDeviceId(pairedDeviceId: String): String? {
        if (shouldThrow) throw RuntimeException("Test error")
        return pairingsFlow.value.find { it.pairedDeviceId == pairedDeviceId }?.id
            ?: "pairing-for-$pairedDeviceId"
    }
}
