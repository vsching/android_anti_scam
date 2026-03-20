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
            // Check if the device has any guardian pairings
            val pairingCount = guardianRepository.getGuardianCount().first()
            if (pairingCount == 0) {
                return Result.success()
            }

            // Get current security score
            val score = auditRepository.getSecurityScore().first()
                ?: return Result.success()

            // Send heartbeat to backend
            guardianRepository.sendHeartbeat(
                securityScore = score.scorePercent,
                securedItems = score.securedItems,
                totalItems = score.totalItems,
                playProtectEnabled = true,
            )

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
