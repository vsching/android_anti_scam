/**
 * Application class for Safe Anot? — the Hilt entry point for dependency injection.
 * Schedules periodic database sync, triggers immediate sync on first run,
 * creates notification channels, and initializes FCM topic subscriptions.
 */
package com.safeanot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safeanot.app.data.local.ReminderConfigDao
import com.safeanot.app.data.local.UserPreferencesDataStore
import com.safeanot.app.data.remote.FcmTokenManager
import com.safeanot.app.domain.repository.SyncRepository
import com.safeanot.app.util.Constants
import com.safeanot.app.worker.AuditReminderScheduler
import com.safeanot.app.worker.DatabaseSyncWorker
import com.safeanot.app.worker.ShareEventSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SafeAnotApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var reminderScheduler: AuditReminderScheduler

    @Inject
    lateinit var reminderConfigDao: ReminderConfigDao

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStore

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleDatabaseSync()
        scheduleAuditReminders()
        scheduleShareEventSync()
        registerFcmToken()
        initializeFcmSubscription()
    }

    /**
     * Create all notification channels at app startup. This is idempotent on API 26+.
     * Channels are created here (single place) rather than scattered across services.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Scam alerts channel
            val scamAlertsChannel = NotificationChannel(
                Constants.SCAM_ALERTS_CHANNEL_ID,
                Constants.SCAM_ALERTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Push notifications for new scam alerts in your region"
            }
            notificationManager.createNotificationChannel(scamAlertsChannel)
        }
    }

    /**
     * Observe scam alert notification preference and region preference,
     * and reactively manage FCM topic subscriptions.
     * When either preference changes, the subscription is updated accordingly.
     */
    private fun initializeFcmSubscription() {
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                userPreferencesDataStore.scamAlertsNotificationsEnabledFlow,
                userPreferencesDataStore.regionFlow,
            ) { enabled, region ->
                Pair(enabled, region)
            }.collect { (enabled, region) ->
                fcmTokenManager.updateSubscription(enabled, region)
            }
        }
    }

    private fun scheduleDatabaseSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Schedule periodic 24-hour sync
        val periodicRequest = PeriodicWorkRequestBuilder<DatabaseSyncWorker>(
            24, TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "safeanot_db_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )

        // Schedule immediate first-run sync if metadata is null
        CoroutineScope(Dispatchers.IO).launch {
            if (syncRepository.isFirstRun()) {
                val immediateRequest = OneTimeWorkRequestBuilder<DatabaseSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(this@SafeAnotApp).enqueueUniqueWork(
                    "safeanot_db_sync_immediate",
                    ExistingWorkPolicy.KEEP,
                    immediateRequest,
                )
            }
        }
    }

    private fun scheduleShareEventSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Periodic sync every 6 hours
        val periodicRequest = PeriodicWorkRequestBuilder<ShareEventSyncWorker>(
            6, TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "safeanot_share_event_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )

        // Immediate startup sync for any queued events
        val immediateRequest = OneTimeWorkRequestBuilder<ShareEventSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "safeanot_share_event_sync_startup",
            ExistingWorkPolicy.KEEP,
            immediateRequest,
        )
    }

    private fun registerFcmToken() {
        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenManager.registerToken()
        }
    }

    private fun scheduleAuditReminders() {
        CoroutineScope(Dispatchers.IO).launch {
            val config = reminderConfigDao.getConfigOnce()
            val enabled = config?.enabled ?: true
            val intervalDays = config?.intervalDays ?: Constants.DEFAULT_REMINDER_INTERVAL_DAYS.toInt()
            reminderScheduler.updateSchedule(enabled, intervalDays)
        }
    }
}
