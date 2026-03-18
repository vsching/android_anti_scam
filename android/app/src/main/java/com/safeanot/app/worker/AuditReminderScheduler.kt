/**
 * Central scheduler for periodic audit reminder work.
 * Manages enqueue/cancel/update of the audit reminder WorkManager periodic work request.
 * Supports interval options of 3, 7, 14, and 30 days.
 */
package com.safeanot.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safeanot.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AuditReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Schedules the audit reminder worker with the given interval.
     * If a reminder is already scheduled, it will be replaced with the new interval.
     *
     * @param intervalDays The interval in days (3, 7, 14, or 30).
     */
    open fun schedule(intervalDays: Int) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AuditReminderWorker>(
            intervalDays.toLong(), TimeUnit.DAYS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.AUDIT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Cancels any scheduled audit reminder worker.
     */
    open fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.AUDIT_WORK_NAME)
    }

    /**
     * Updates the reminder schedule based on the enabled state and interval.
     *
     * @param enabled Whether reminders are enabled.
     * @param intervalDays The interval in days.
     */
    open fun updateSchedule(enabled: Boolean, intervalDays: Int) {
        if (enabled) {
            schedule(intervalDays)
        } else {
            cancel()
        }
    }
}
