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
    @SerializedName("label") val label: String,
)

data class GuardianPairingDto(
    @SerializedName("id") val id: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("paired_device_id") val pairedDeviceId: String,
    @SerializedName("role") val role: String,
    @SerializedName("label") val label: String,
    @SerializedName("created_at") val createdAt: Long,
) {
    fun toDomain(): GuardianPairing = GuardianPairing(
        id = id,
        deviceId = deviceId,
        pairedDeviceId = pairedDeviceId,
        role = GuardianRole.valueOf(role),
        label = label,
        createdAt = createdAt,
    )
}

data class DeletePairingRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("pairing_id") val pairingId: String,
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

data class WardHeartbeatDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("security_score") val securityScore: Int,
    @SerializedName("secured_items") val securedItems: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("play_protect_enabled") val playProtectEnabled: Boolean,
    @SerializedName("timestamp") val timestamp: Long,
)
