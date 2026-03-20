/**
 * Firebase Cloud Messaging service for receiving push notifications.
 * Handles guardian alert notifications and FCM token refresh.
 */
package com.safeanot.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safeanot.app.MainActivity
import com.safeanot.app.R
import com.safeanot.app.data.remote.FcmTokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SafeAnotMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    companion object {
        private const val CHANNEL_ID_GUARDIAN = "guardian_alerts"
        private const val CHANNEL_NAME_GUARDIAN = "Guardian Alerts"
        private const val NOTIFICATION_ID_BASE = 5000
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenManager.registerToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: return

        when (type) {
            "guardian_alert" -> showGuardianAlertNotification(data)
            "help_request" -> showGuardianAlertNotification(data)
        }
    }

    /** Sanitize notification text: strip newlines and truncate to maxLength. */
    private fun sanitize(text: String, maxLength: Int = 100): String {
        return text.replace(Regex("[\r\n]+"), " ").take(maxLength)
    }

    private fun showGuardianAlertNotification(data: Map<String, String>) {
        createGuardianNotificationChannel()

        val wardName = sanitize(data["ward_name"] ?: "Your ward")
        val alertReason = sanitize(data["alert_reason"] ?: "Security status changed")
        val wardDeviceId = data["ward_device_id"] ?: ""
        val type = data["type"] ?: "guardian_alert"

        val title = if (type == "help_request") {
            "$wardName needs help"
        } else {
            "$wardName — Security Alert"
        }

        // Deep link to Guardian Dashboard
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "guardian_dashboard")
            putExtra("ward_device_id", wardDeviceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            wardDeviceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_GUARDIAN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(alertReason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertReason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            NOTIFICATION_ID_BASE + wardDeviceId.hashCode(),
            notification,
        )
    }

    private fun createGuardianNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_GUARDIAN,
                CHANNEL_NAME_GUARDIAN,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts about your family members' phone security"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
