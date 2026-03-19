/**
 * WorkManager worker that periodically syncs unsynced share events to the backend.
 */
package com.safeanot.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeanot.app.domain.repository.ShareEventRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ShareEventSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val shareEventRepository: ShareEventRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val synced = shareEventRepository.syncPendingEvents()
            if (synced) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
