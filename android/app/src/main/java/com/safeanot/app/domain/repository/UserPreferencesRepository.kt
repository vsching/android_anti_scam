/**
 * Repository contract for user preferences (region selection and notification settings).
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.AlertRegionFilter
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    /**
     * Observe the user's preferred region. Falls back to locale-based detection
     * when no explicit preference has been set.
     */
    fun getRegion(): Flow<AlertRegionFilter>

    /**
     * Persist the user's preferred region.
     */
    suspend fun setRegion(region: AlertRegionFilter)

    /**
     * Observe whether scam alert notifications are enabled.
     */
    fun getScamAlertsEnabled(): Flow<Boolean>

    /**
     * Persist the scam alert notification preference.
     */
    suspend fun setScamAlertsEnabled(enabled: Boolean)
}
