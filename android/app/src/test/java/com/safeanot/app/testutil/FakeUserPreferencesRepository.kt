package com.safeanot.app.testutil

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    val regionFlow = MutableStateFlow(AlertRegionFilter.ALL)
    val scamAlertsFlow = MutableStateFlow(true)

    override fun getRegion(): Flow<AlertRegionFilter> = regionFlow
    override suspend fun setRegion(region: AlertRegionFilter) { regionFlow.value = region }
    override fun getScamAlertsEnabled(): Flow<Boolean> = scamAlertsFlow
    override suspend fun setScamAlertsEnabled(enabled: Boolean) { scamAlertsFlow.value = enabled }
}
