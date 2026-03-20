/**
 * Data classes for guardian pairing API request and response models.
 */
package com.safeanot.app.data.remote.model

import com.google.gson.annotations.SerializedName
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.GuardianRole
import com.safeanot.app.domain.model.PairingCode

data class GeneratePairingCodeRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("role") val role: String,
    @SerializedName("label") val label: String,
)

data class PairingCodeResponse(
    @SerializedName("code") val code: String,
    @SerializedName("expires_at") val expiresAt: Long,
) {
    fun toDomain(): PairingCode = PairingCode(
        code = code,
        expiresAt = expiresAt,
    )
}

data class ClaimPairingCodeRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("code") val code: String,
    @SerializedName("display_name") val label: String,
)

data class GuardianPairingDto(
    @SerializedName("id") val id: Long,
    @SerializedName("ward_device_id") val wardDeviceId: String? = null,
    @SerializedName("guardian_device_id") val guardianDeviceId: String? = null,
    @SerializedName("ward_display_name") val wardDisplayName: String? = null,
    @SerializedName("guardian_display_name") val guardianDisplayName: String? = null,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("security_score") val securityScore: Int? = null,
    @SerializedName("heartbeat_timestamp") val heartbeatTimestamp: Long? = null,
    @SerializedName("play_protect_enabled") val playProtectEnabled: Boolean? = null,
) {
    /**
     * Maps DTO to domain model. [myDeviceId] is used to derive the role:
     * if my device is the ward_device_id, I am the WARD; otherwise I am the GUARDIAN.
     */
    fun toDomain(myDeviceId: String): GuardianPairing {
        val role = if (myDeviceId == wardDeviceId) GuardianRole.WARD else GuardianRole.GUARDIAN
        val pairedDeviceId = if (role == GuardianRole.WARD) guardianDeviceId else wardDeviceId
        val label = if (role == GuardianRole.WARD) {
            guardianDisplayName ?: ""
        } else {
            wardDisplayName ?: ""
        }
        return GuardianPairing(
            id = id.toString(),
            deviceId = myDeviceId,
            pairedDeviceId = pairedDeviceId ?: "",
            role = role,
            label = label,
            createdAt = createdAt,
            lastSecurityScore = securityScore,
            lastHeartbeatAt = heartbeatTimestamp,
            playProtectEnabled = playProtectEnabled,
        )
    }
}

data class DeletePairingRequest(
    @SerializedName("device_id") val deviceId: String,
)

data class HeartbeatRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("security_score") val securityScore: Int,
    @SerializedName("secured_items") val securedItems: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("play_protect_enabled") val playProtectEnabled: Boolean,
    @SerializedName("timestamp") val timestamp: Long,
)

data class RegisterFcmTokenRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("fcm_token") val fcmToken: String,
)

data class HelpRequestBody(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("security_score") val securityScore: Int,
    @SerializedName("unfixed_items") val unfixedItems: List<String>,
)

data class WardHeartbeatDto(
    @SerializedName("security_score") val securityScore: Int,
    @SerializedName("secured_items") val securedItems: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("play_protect_enabled") val playProtectEnabled: Boolean,
    @SerializedName("timestamp") val timestamp: Long,
)
