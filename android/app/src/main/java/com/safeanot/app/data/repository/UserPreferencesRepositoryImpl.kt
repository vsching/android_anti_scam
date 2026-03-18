/**
 * Repository implementation for user preferences backed by DataStore.
 * Falls back to locale-based region detection when no preference is stored.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.UserPreferencesDataStore
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.UserPreferencesRepository
import com.safeanot.app.util.RegionResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
) : UserPreferencesRepository {

    override fun getRegion(): Flow<AlertRegionFilter> {
        return dataStore.regionFlow.map { stored ->
            if (stored != null) {
                stringToRegionFilter(stored)
            } else {
                RegionResolver.fromLocale()
            }
        }
    }

    override suspend fun setRegion(region: AlertRegionFilter) {
        dataStore.setRegion(region.name)
    }

    override fun getScamAlertsEnabled(): Flow<Boolean> {
        return dataStore.scamAlertsNotificationsEnabledFlow
    }

    override suspend fun setScamAlertsEnabled(enabled: Boolean) {
        dataStore.setScamAlertsNotificationsEnabled(enabled)
    }

    private fun stringToRegionFilter(value: String): AlertRegionFilter {
        return try {
            AlertRegionFilter.valueOf(value)
        } catch (_: IllegalArgumentException) {
            AlertRegionFilter.ALL
        }
    }
}
