/**
 * Repository interface for database synchronization operations.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.SyncOutcome

interface SyncRepository {
    suspend fun syncDatabase(): SyncOutcome
    suspend fun getLastSyncVersion(): String?
    suspend fun isFirstRun(): Boolean
}
