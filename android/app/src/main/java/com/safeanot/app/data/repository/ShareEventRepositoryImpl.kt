/**
 * Concrete implementation of ShareEventRepository.
 * Persists share events locally, applies rate limiting (100/day),
 * and syncs to the backend with immediate attempt + offline queue.
 */
package com.safeanot.app.data.repository

import android.content.Context
import com.safeanot.app.data.local.ShareEventDao
import com.safeanot.app.data.local.entity.ShareEventEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.ShareEventBatchRequest
import com.safeanot.app.data.remote.model.ShareEventDto
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.repository.ShareEventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

class ShareEventRepositoryImpl @Inject constructor(
    private val shareEventDao: ShareEventDao,
    private val api: SafeAnotApi,
    @ApplicationContext private val context: Context,
) : ShareEventRepository {

    companion object {
        private const val DAILY_RATE_LIMIT = 100
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        private const val PREFS_NAME = "safeanot_device"
        private const val KEY_DEVICE_ID = "device_id"
    }

    override suspend fun trackEvent(event: ShareEventModel): Boolean {
        // Rate limit: max 100 events per day
        val oneDayAgo = System.currentTimeMillis() - MILLIS_PER_DAY
        val recentCount = shareEventDao.countSince(oneDayAgo)
        if (recentCount >= DAILY_RATE_LIMIT) {
            return false
        }

        val entity = ShareEventEntity(
            shareType = event.shareType.name,
            contentId = event.contentId,
            platform = event.platform.name,
            timestamp = event.timestamp,
        )
        shareEventDao.insert(entity)

        // Attempt immediate sync — fail silently if offline
        try {
            syncPendingEvents()
        } catch (_: Exception) {
            // Will be retried by periodic worker
        }

        return true
    }

    override suspend fun syncPendingEvents(): Boolean {
        // Purge stale unsynced events older than backend's 30-day window
        // to prevent sync deadlock (backend rejects, same rows retried forever)
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * MILLIS_PER_DAY)
        shareEventDao.deleteStaleUnsynced(thirtyDaysAgo)

        val unsynced = shareEventDao.getUnsynced()
        if (unsynced.isEmpty()) return true

        val deviceId = getOrCreateDeviceId()
        val dtos = unsynced.map { entity ->
            ShareEventDto(
                shareType = entity.shareType,
                contentId = entity.contentId,
                platform = entity.platform,
                timestamp = entity.timestamp,
            )
        }

        val request = ShareEventBatchRequest(
            deviceId = deviceId,
            events = dtos,
        )

        val response = api.postShareEvents(request)
        if (response.isSuccessful) {
            shareEventDao.markSynced(unsynced.map { it.id })
            // Clean up old synced events (older than 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - (7 * MILLIS_PER_DAY)
            shareEventDao.deleteOldSynced(sevenDaysAgo)
            return true
        }
        return false
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }
}
