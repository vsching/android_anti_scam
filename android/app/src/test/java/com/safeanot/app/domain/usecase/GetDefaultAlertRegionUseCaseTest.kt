package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDefaultAlertRegionUseCaseTest {

    @Test
    fun `invoke delegates to repository`() {
        val fakeRepo = object : AlertsRepository {
            override fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>> = emptyFlow()
            override suspend fun refreshAlerts(filter: AlertRegionFilter) {}
            override suspend fun getAlertById(id: String): ScamAlert? = null
            override fun getDefaultRegionFilter(): AlertRegionFilter = AlertRegionFilter.SINGAPORE
        }
        val useCase = GetDefaultAlertRegionUseCase(fakeRepo)
        assertEquals(AlertRegionFilter.SINGAPORE, useCase())
    }
}
