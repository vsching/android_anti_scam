/**
 * Repository interface for share event analytics.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.ShareEventModel

interface ShareEventRepository {

    /**
     * Record a share event. Persists locally and attempts immediate sync.
     * Rate-limited to 100 events per day.
     * @return true if the event was recorded, false if rate-limited.
     */
    suspend fun trackEvent(event: ShareEventModel): Boolean

    /**
     * Sync any unsynced share events to the backend.
     * @return true if all events were synced successfully.
     */
    suspend fun syncPendingEvents(): Boolean
}
