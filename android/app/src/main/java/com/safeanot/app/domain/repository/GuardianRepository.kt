/**
 * Repository interface for guardian pairing operations.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.PairingCode
import kotlinx.coroutines.flow.Flow

interface GuardianRepository {

    /**
     * Generate a new pairing code for the current device.
     * @param role "GUARDIAN" or "WARD" — the role the code generator wants to assume.
     * @param label Human-readable label for this device (e.g., "Dad's Phone").
     */
    suspend fun generatePairingCode(role: String, label: String): PairingCode

    /**
     * Claim an existing pairing code to establish a relationship.
     * @param code The pairing code to claim.
     * @param label Human-readable label for this device.
     * @return The created pairing relationship.
     */
    suspend fun claimPairingCode(code: String, label: String): GuardianPairing

    /**
     * Get all pairings for the current device (both guardian and ward roles).
     */
    fun getAllPairings(): Flow<List<GuardianPairing>>

    /**
     * Delete a pairing relationship.
     */
    suspend fun deletePairing(pairingId: String)

    /**
     * Refresh pairings from the backend and update local cache.
     */
    suspend fun refreshPairings()

    /**
     * Get the total count of pairings as a Flow.
     */
    fun getGuardianCount(): Flow<Int>

    /**
     * Get the device ID for the current device.
     */
    suspend fun getDeviceId(): String

    /**
     * Send a security score heartbeat to the backend.
     * @param securityScore The overall security score (0-100).
     * @param securedItems Number of items that are secured.
     * @param totalItems Total number of audited items.
     * @param playProtectEnabled Whether Play Protect is enabled on the device.
     */
    suspend fun sendHeartbeat(
        securityScore: Int,
        securedItems: Int,
        totalItems: Int,
        playProtectEnabled: Boolean,
    )

    /**
     * Get wards (devices this user is monitoring) as a Flow.
     * Filters pairings where role is GUARDIAN.
     */
    fun getWards(deviceId: String): Flow<List<GuardianPairing>>

    /**
     * Refresh wards from the backend and update local cache.
     */
    suspend fun refreshWards(deviceId: String)

    /**
     * Get heartbeat history for a specific ward.
     * @param wardDeviceId The device ID of the ward.
     * @param days Number of days of history to fetch.
     */
    suspend fun getWardHeartbeatHistory(wardDeviceId: String, days: Int = 7): com.safeanot.app.domain.model.WardHeartbeatHistory
}
