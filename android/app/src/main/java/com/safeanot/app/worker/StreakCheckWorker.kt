/**
 * WorkManager worker for periodic streak checks.
 * Gets the current security score, updates the streak, and shows notifications
 * for any newly unlocked badges.
 */
package com.safeanot.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeanot.app.MainActivity
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.usecase.UpdateStreakUseCase
import com.safeanot.app.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val auditRepository: AuditRepository,
    private val updateStreakUseCase: UpdateStreakUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val score = auditRepository.getSecurityScore().firstOrNull()
            // Skip streak update if no audit baseline exists yet (new user).
            // A synthetic 0 score would write lastCheckDate and block real same-day streak starts.
            if (score == null) return Result.success()
            val newBadges = updateStreakUseCase(score.scorePercent)

            if (newBadges.isNotEmpty() && hasNotificationPermission()) {
                createNotificationChannel()
                for (badge in newBadges) {
                    sendBadgeNotification(badge)
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Streak and badge notifications"
        }

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendBadgeNotification(badge: BadgeType) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val badgeName = badge.name.replace('_', ' ').lowercase()
            .replaceFirstChar { it.uppercase() }

        val notification = NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Badge Unlocked!")
            .setContentText("You earned the \"$badgeName\" badge!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(BADGE_NOTIFICATION_BASE_ID + badge.ordinal, notification)
    }

    companion object {
        private const val BADGE_NOTIFICATION_BASE_ID = 2000
    }
}
