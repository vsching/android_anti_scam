/**
 * Scheduler for the periodic guardian heartbeat worker.
 * Sends security score heartbeats every 6 hours when the device has network connectivity.
 */
package com.safeanot.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class GuardianHeartbeatScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Schedules the heartbeat worker to run every 6 hours.
     * Requires network connectivity.
     */
    open fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<GuardianHeartbeatWorker>(
            HEARTBEAT_INTERVAL_HOURS, TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HEARTBEAT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Cancels the heartbeat worker.
     */
    open fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(HEARTBEAT_WORK_NAME)
    }

    companion object {
        const val HEARTBEAT_WORK_NAME = "guardian_heartbeat"
        const val HEARTBEAT_INTERVAL_HOURS = 6L
    }
}
