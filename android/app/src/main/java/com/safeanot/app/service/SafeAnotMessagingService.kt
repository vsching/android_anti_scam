/**
 * Firebase Cloud Messaging service for receiving push notifications.
 * Handles guardian alert notifications, scam alert notifications, and FCM token refresh.
 *
 * Notification channels are created in SafeAnotApp.onCreate() — NOT here.
 */
package com.safeanot.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safeanot.app.MainActivity
import com.safeanot.app.R
import com.safeanot.app.data.remote.FcmTokenManager
import com.safeanot.app.util.Constants
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
        private const val SCAM_ALERT_NOTIFICATION_ID_BASE = 6000
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
            "scam_alert" -> showScamAlertNotification(data)
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

    /**
     * Show a scam alert push notification.
     * Deep-links to the alert detail screen via safeanot.com/alert/{alertId}.
     */
    private fun showScamAlertNotification(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val title = sanitize(data["title"] ?: "New Scam Alert")
        val body = sanitize(data["body"] ?: "A new scam alert has been reported in your region.", 200)

        // Deep link to alert detail screen
        val deepLinkUri = Uri.parse("https://safeanot.com/alert/$alertId")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, Constants.SCAM_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            SCAM_ALERT_NOTIFICATION_ID_BASE + alertId.hashCode(),
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
