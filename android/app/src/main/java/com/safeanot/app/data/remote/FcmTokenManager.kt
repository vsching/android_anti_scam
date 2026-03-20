/**
 * Manages FCM token registration with the backend and topic subscriptions
 * for scam alert push notifications based on user region preference.
 */
package com.safeanot.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.safeanot.app.data.remote.model.RegisterFcmTokenRequest
import com.safeanot.app.util.Constants
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

        /** All known scam alert topics for unsubscribe-all operations. */
        internal val ALL_SCAM_ALERT_TOPICS = listOf(
            Constants.FCM_TOPIC_SCAM_ALERTS_MY,
            Constants.FCM_TOPIC_SCAM_ALERTS_SG,
        )
    }

    /** Tracks the currently subscribed topic to avoid redundant subscribe/unsubscribe calls. */
    @Volatile
    private var currentTopic: String? = null

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

    /**
     * Subscribe to a region-based scam alerts topic.
     * Maps region string (e.g., "MY", "SG") to topic name (e.g., "scam_alerts_MY").
     * If region is null, no subscription is made.
     */
    fun subscribeToRegionTopic(region: String?) {
        if (region == null) {
            Log.d(TAG, "Region is null, skipping topic subscription")
            return
        }
        val topic = "${Constants.FCM_TOPIC_PREFIX}$region"
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnSuccessListener {
                currentTopic = topic
                Log.d(TAG, "Subscribed to topic: $topic")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            }
    }

    /**
     * Unsubscribe from all known scam alert topics.
     */
    fun unsubscribeFromAllTopics() {
        ALL_SCAM_ALERT_TOPICS.forEach { topic ->
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnSuccessListener {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
                }
        }
        currentTopic = null
    }

    /**
     * Update FCM topic subscription based on the notification toggle and selected region.
     *
     * - When [enabled] is true and [region] is non-null: unsubscribe from all topics,
     *   then subscribe to the region-specific topic.
     * - When [enabled] is false: unsubscribe from all topics.
     * - When [region] is null: unsubscribe from all topics (no region selected yet).
     */
    fun updateSubscription(enabled: Boolean, region: String?) {
        if (!enabled || region == null) {
            unsubscribeFromAllTopics()
            return
        }

        val newTopic = "${Constants.FCM_TOPIC_PREFIX}$region"
        if (newTopic == currentTopic) {
            Log.d(TAG, "Already subscribed to $newTopic, skipping")
            return
        }

        // Unsubscribe from old topics before subscribing to new one
        unsubscribeFromAllTopics()
        subscribeToRegionTopic(region)
    }
}
