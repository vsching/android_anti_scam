/**
 * Domain models for ward heartbeat history.
 * Used to display security score trends on the Guardian Dashboard.
 */
package com.safeanot.app.domain.model

/**
 * Historical heartbeat data for a ward device.
 */
data class WardHeartbeatHistory(
    val deviceId: String,
    val displayName: String,
    val heartbeats: List<HeartbeatEntry>,
)

/**
 * A single heartbeat entry recording the ward's security state at a point in time.
 */
data class HeartbeatEntry(
    val securityScore: Int,
    val securedItems: Int,
    val totalItems: Int,
    val playProtectEnabled: Boolean,
    val timestamp: Long,
)
