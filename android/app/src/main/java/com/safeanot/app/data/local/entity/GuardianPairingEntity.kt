/**
 * Room entity for persisting guardian pairing relationships locally.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole

@Entity(tableName = "guardian_pairings")
data class GuardianPairingEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "paired_device_id")
    val pairedDeviceId: String,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "last_security_score", defaultValue = "NULL")
    val lastSecurityScore: Int? = null,

    @ColumnInfo(name = "last_heartbeat_at", defaultValue = "NULL")
    val lastHeartbeatAt: Long? = null,

    @ColumnInfo(name = "play_protect_enabled", defaultValue = "NULL")
    val playProtectEnabled: Boolean? = null,
) {
    fun toDomain(): GuardianPairing = GuardianPairing(
        id = id,
        deviceId = deviceId,
        pairedDeviceId = pairedDeviceId,
        role = GuardianRole.valueOf(role),
        label = label,
        createdAt = createdAt,
        lastSecurityScore = lastSecurityScore,
        lastHeartbeatAt = lastHeartbeatAt,
        playProtectEnabled = playProtectEnabled,
    )

    companion object {
        fun fromDomain(domain: GuardianPairing): GuardianPairingEntity =
            GuardianPairingEntity(
                id = domain.id,
                deviceId = domain.deviceId,
                pairedDeviceId = domain.pairedDeviceId,
                role = domain.role.name,
                label = domain.label,
                createdAt = domain.createdAt,
                lastSecurityScore = domain.lastSecurityScore,
                lastHeartbeatAt = domain.lastHeartbeatAt,
                playProtectEnabled = domain.playProtectEnabled,
            )
    }
}
