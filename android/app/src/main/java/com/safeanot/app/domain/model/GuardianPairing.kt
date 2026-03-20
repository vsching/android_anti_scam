/**
 * Domain models for guardian pairing feature.
 * A guardian can monitor wards' security status, and a ward is protected by guardians.
 */
package com.safeanot.app.domain.model

/**
 * Role in a guardian pairing relationship.
 */
enum class GuardianRole {
    /** The person doing the monitoring (e.g., a parent). */
    GUARDIAN,
    /** The person being monitored/protected (e.g., a child or elderly parent). */
    WARD,
}

/**
 * A pairing code used to establish a guardian-ward relationship.
 */
data class PairingCode(
    val code: String,
    val expiresAt: Long,
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}

/**
 * A guardian pairing relationship between two devices.
 */
data class GuardianPairing(
    val id: String,
    val deviceId: String,
    val pairedDeviceId: String,
    val role: GuardianRole,
    val label: String,
    val createdAt: Long,
)
