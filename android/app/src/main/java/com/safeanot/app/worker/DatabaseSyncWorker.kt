/**
 * WorkManager worker that performs periodic database synchronization.
 * Maps SyncOutcome to WorkManager Result for proper retry/failure handling.
 */
package com.safeanot.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeanot.app.domain.model.SyncOutcome
import com.safeanot.app.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DatabaseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (val outcome = syncRepository.syncDatabase()) {
            is SyncOutcome.Success -> Result.success()
            is SyncOutcome.Retry -> Result.retry()
            is SyncOutcome.PermanentFailure -> Result.failure()
        }
    }
}
