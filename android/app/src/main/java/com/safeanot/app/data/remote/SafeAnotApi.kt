/**
 * Retrofit interface for the Safe Anot? backend API.
 */
package com.safeanot.app.data.remote

import com.safeanot.app.data.remote.model.CheckRequest
import com.safeanot.app.data.remote.model.CheckResponse
import com.safeanot.app.data.remote.model.ClaimPairingCodeRequest
import com.safeanot.app.data.remote.model.DeletePairingRequest
import com.safeanot.app.data.remote.model.GeneratePairingCodeRequest
import com.safeanot.app.data.remote.model.HeartbeatRequest
import com.safeanot.app.data.remote.model.HelpRequestBody
import com.safeanot.app.data.remote.model.RegisterFcmTokenRequest
import com.safeanot.app.data.remote.model.GuardianPairingDto
import com.safeanot.app.data.remote.model.WardHeartbeatDto
import com.safeanot.app.data.remote.model.LatestMetadataResponse
import com.safeanot.app.data.remote.model.PairingCodeResponse
import okhttp3.ResponseBody
import com.safeanot.app.data.remote.model.AlertDto
import com.safeanot.app.data.remote.model.ShareEventBatchRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SafeAnotApi {

    @GET("api/data/latest")
    suspend fun getLatestMetadata(): LatestMetadataResponse

    @Streaming
    @GET("api/data/full")
    suspend fun getFullDatabase(): ResponseBody

    @Streaming
    @GET("api/data/delta")
    suspend fun getDelta(@Query("since") since: String): ResponseBody

    @Streaming
    @GET("api/data/bloom")
    suspend fun getBloomFilter(): ResponseBody

    @POST("api/check")
    suspend fun checkDomain(@Body request: CheckRequest): CheckResponse

    @GET("api/alerts")
    suspend fun getAlerts(@Query("region") region: String? = null): List<AlertDto>

    @POST("api/score/share")
    suspend fun postShareEvents(@Body request: ShareEventBatchRequest): Response<Unit>

    @POST("api/guardian/pair/generate")
    suspend fun generatePairingCode(@Body request: GeneratePairingCodeRequest): PairingCodeResponse

    @POST("api/guardian/pair/claim")
    suspend fun claimPairingCode(@Body request: ClaimPairingCodeRequest): GuardianPairingDto

    @GET("api/guardian/wards")
    suspend fun getWards(@Query("device_id") deviceId: String): List<GuardianPairingDto>

    @GET("api/guardian/guardians")
    suspend fun getGuardians(@Query("device_id") deviceId: String): List<GuardianPairingDto>

    @HTTP(method = "DELETE", path = "api/guardian/pair/{pairingId}", hasBody = false)
    suspend fun deletePairing(@retrofit2.http.Path("pairingId") pairingId: Long): Response<Unit>

    @POST("api/guardian/heartbeat")
    suspend fun postHeartbeat(@Body request: HeartbeatRequest): Response<Unit>

    @POST("api/guardian/fcm-token")
    suspend fun registerFcmToken(@Body request: RegisterFcmTokenRequest): Response<Unit>

    @GET("api/guardian/wards/{deviceId}/heartbeats")
    suspend fun getWardHeartbeats(
        @retrofit2.http.Path("deviceId") wardDeviceId: String,
        @Query("days") days: Int = 7,
        @Query("guardian_device_id") guardianDeviceId: String,
    ): List<WardHeartbeatDto>

    @POST("api/guardian/help-request")
    suspend fun sendHelpRequest(@Body request: HelpRequestBody): Response<Unit>
}
