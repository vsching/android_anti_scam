/**
 * WorkManager worker that periodically sends a security score heartbeat
 * to the backend for guardian monitoring.
 * Only sends if the device has active guardian pairings.
 */
package com.safeanot.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeanot.app.domain.model.AppCategory
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.GuardianRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class GuardianHeartbeatWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val guardianRepository: GuardianRepository,
    private val auditRepository: AuditRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Only send heartbeats if this device is a ward (has guardians monitoring it)
            val wardRoleCount = guardianRepository.getWardRoleCount().first()
            if (wardRoleCount == 0) {
                return Result.success()
            }

            // Get current security score
            val score = auditRepository.getSecurityScore().first()
                ?: return Result.success()

            // Determine Play Protect status from PROTECTION category audit items.
            // If all protection items are SECURED or NOT_INSTALLED, consider Play Protect enabled.
            val protectionItems = auditRepository.getAuditItemsByCategory(
                AppCategory.PROTECTION.name,
            ).first()
            val playProtectEnabled = protectionItems.isEmpty() || protectionItems.all {
                it.status == AuditStatus.SECURED || it.status == AuditStatus.NOT_INSTALLED
            }

            // Send heartbeat to backend
            guardianRepository.sendHeartbeat(
                securityScore = score.scorePercent,
                securedItems = score.securedItems,
                totalItems = score.totalItems,
                playProtectEnabled = playProtectEnabled,
            )

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
