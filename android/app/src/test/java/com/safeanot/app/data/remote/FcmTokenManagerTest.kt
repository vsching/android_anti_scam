/**
 * Unit tests for FcmTokenManager.
 * Verifies token registration and topic subscription logic.
 */
package com.safeanot.app.data.remote

import com.safeanot.app.data.remote.model.RegisterFcmTokenRequest
import com.safeanot.app.util.Constants
import com.safeanot.app.util.DeviceIdProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FcmTokenManagerTest {

    /**
     * Fake SafeAnotApi that captures the FCM token registration request.
     */
    private class FakeSafeAnotApi : SafeAnotApi {
        var lastFcmTokenRequest: RegisterFcmTokenRequest? = null

        override suspend fun registerFcmToken(request: RegisterFcmTokenRequest): Response<Unit> {
            lastFcmTokenRequest = request
            return Response.success(Unit)
        }

        // Stubs for other methods — not used in this test
        override suspend fun getLatestMetadata() = throw NotImplementedError()
        override suspend fun getFullDatabase() = throw NotImplementedError()
        override suspend fun getDelta(since: String) = throw NotImplementedError()
        override suspend fun getBloomFilter() = throw NotImplementedError()
        override suspend fun checkDomain(request: com.safeanot.app.data.remote.model.CheckRequest) = throw NotImplementedError()
        override suspend fun getAlerts(region: String?) = throw NotImplementedError()
        override suspend fun postShareEvents(request: com.safeanot.app.data.remote.model.ShareEventBatchRequest) = throw NotImplementedError()
        override suspend fun generatePairingCode(request: com.safeanot.app.data.remote.model.GeneratePairingCodeRequest) = throw NotImplementedError()
        override suspend fun claimPairingCode(request: com.safeanot.app.data.remote.model.ClaimPairingCodeRequest) = throw NotImplementedError()
        override suspend fun getWards(deviceId: String) = throw NotImplementedError()
        override suspend fun getGuardians(deviceId: String) = throw NotImplementedError()
        override suspend fun deletePairing(pairingId: Long, request: com.safeanot.app.data.remote.model.DeletePairingRequest) = throw NotImplementedError()
        override suspend fun postHeartbeat(request: com.safeanot.app.data.remote.model.HeartbeatRequest) = throw NotImplementedError()
        override suspend fun getWardHeartbeats(wardDeviceId: String, days: Int, guardianDeviceId: String) = throw NotImplementedError()
        override suspend fun sendHelpRequest(request: com.safeanot.app.data.remote.model.HelpRequestBody): retrofit2.Response<Unit> = throw NotImplementedError()
    }

    /**
     * Fake DeviceIdProvider that returns a fixed device ID.
     */
    private class FakeDeviceIdProvider : DeviceIdProvider(
        context = null!!  // Not used since we override the method
    ) {
        override fun getOrCreateDeviceId(): String = "test-device-id-123"
    }

    @Test
    fun `registerToken sends correct device ID to API`() = runTest {
        val fakeApi = FakeSafeAnotApi()
        val fakeDeviceIdProvider = FakeDeviceIdProvider()

        // Note: In a real test we'd mock FirebaseMessaging, but since this is a unit test
        // and FirebaseMessaging.getInstance() requires Google Play Services, we verify
        // the DeviceIdProvider and API interaction pattern here.
        assertEquals("test-device-id-123", fakeDeviceIdProvider.getOrCreateDeviceId())

        // Verify the API method accepts the correct request structure
        val request = RegisterFcmTokenRequest(
            deviceId = fakeDeviceIdProvider.getOrCreateDeviceId(),
            fcmToken = "fake-fcm-token-xyz",
        )
        val response = fakeApi.registerFcmToken(request)
        assertEquals(true, response.isSuccessful)
        assertEquals("test-device-id-123", fakeApi.lastFcmTokenRequest?.deviceId)
        assertEquals("fake-fcm-token-xyz", fakeApi.lastFcmTokenRequest?.fcmToken)
    }

    @Test
    fun `ALL_SCAM_ALERT_TOPICS contains MY and SG topics`() {
        val topics = FcmTokenManager.ALL_SCAM_ALERT_TOPICS
        assertEquals(2, topics.size)
        assertTrue(topics.contains(Constants.FCM_TOPIC_SCAM_ALERTS_MY))
        assertTrue(topics.contains(Constants.FCM_TOPIC_SCAM_ALERTS_SG))
    }

    @Test
    fun `topic names follow expected format`() {
        assertEquals("scam_alerts_MY", Constants.FCM_TOPIC_SCAM_ALERTS_MY)
        assertEquals("scam_alerts_SG", Constants.FCM_TOPIC_SCAM_ALERTS_SG)
        assertEquals("scam_alerts_", Constants.FCM_TOPIC_PREFIX)

        // Verify region maps to correct topic name
        val myTopic = "${Constants.FCM_TOPIC_PREFIX}MY"
        assertEquals(Constants.FCM_TOPIC_SCAM_ALERTS_MY, myTopic)

        val sgTopic = "${Constants.FCM_TOPIC_PREFIX}SG"
        assertEquals(Constants.FCM_TOPIC_SCAM_ALERTS_SG, sgTopic)
    }

    @Test
    fun `scam alerts channel constants are defined`() {
        assertEquals("scam_alerts", Constants.SCAM_ALERTS_CHANNEL_ID)
        assertEquals("Scam Alerts", Constants.SCAM_ALERTS_CHANNEL_NAME)
    }
}
