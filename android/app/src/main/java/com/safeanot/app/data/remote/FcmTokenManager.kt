/**
 * Manages FCM token registration with the backend.
 * Called on app startup and whenever the FCM token refreshes.
 */
package com.safeanot.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.safeanot.app.data.remote.model.RegisterFcmTokenRequest
import com.safeanot.app.util.DeviceIdProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val api: SafeAnotApi,
    private val deviceIdProvider: DeviceIdProvider,
) {
    companion object {
        private const val TAG = "FcmTokenManager"
    }

    /**
     * Gets the current FCM token and registers it with the backend.
     * Safe to call multiple times; the backend upserts the token.
     */
    suspend fun registerToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val deviceId = deviceIdProvider.getOrCreateDeviceId()
            val request = RegisterFcmTokenRequest(
                deviceId = deviceId,
                fcmToken = fcmToken,
            )
            val response = api.registerFcmToken(request)
            if (response.isSuccessful) {
                Log.d(TAG, "FCM token registered successfully")
            } else {
                Log.w(TAG, "FCM token registration failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
        }
    }
}
