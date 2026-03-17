/**
 * Repository contract for scam alerts data.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import kotlinx.coroutines.flow.Flow

interface AlertsRepository {

    /**
     * Observe alerts for the given region filter as a reactive stream.
     */
    fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>>

    /**
     * Refresh alerts from the API for the given region filter.
     * Caches results locally. Returns success or throws on failure.
     */
    suspend fun refreshAlerts(filter: AlertRegionFilter)

    /**
     * Get a single alert by ID from the local cache.
     */
    suspend fun getAlertById(id: String): ScamAlert?

    /**
     * Detect the default region filter based on device locale.
     */
    fun getDefaultRegionFilter(): AlertRegionFilter
}
