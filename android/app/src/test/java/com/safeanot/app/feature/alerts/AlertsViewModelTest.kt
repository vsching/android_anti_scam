package com.safeanot.app.feature.alerts

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.usecase.GetDefaultAlertRegionUseCase
import com.safeanot.app.domain.usecase.ObserveAlertsUseCase
import com.safeanot.app.domain.usecase.RefreshAlertsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeAlertsRepository
    private lateinit var viewModel: AlertsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAlertsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AlertsViewModel {
        return AlertsViewModel(
            observeAlertsUseCase = ObserveAlertsUseCase(fakeRepository),
            refreshAlertsUseCase = RefreshAlertsUseCase(fakeRepository),
            getDefaultAlertRegionUseCase = GetDefaultAlertRegionUseCase(fakeRepository),
        )
    }

    @Test
    fun `initial state uses default region filter`() {
        fakeRepository.defaultFilter = AlertRegionFilter.MALAYSIA
        viewModel = createViewModel()

        assertEquals(AlertRegionFilter.MALAYSIA, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `initial load sets isInitialLoading to false after refresh`() {
        viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun `onFilterChanged updates selected filter`() {
        viewModel = createViewModel()

        viewModel.onFilterChanged(AlertRegionFilter.SINGAPORE)

        assertEquals(AlertRegionFilter.SINGAPORE, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `onFilterChanged with same filter is no-op`() {
        fakeRepository.defaultFilter = AlertRegionFilter.ALL
        viewModel = createViewModel()
        val initialRefreshCount = fakeRepository.refreshCount

        viewModel.onFilterChanged(AlertRegionFilter.ALL)

        // Should not have triggered an additional refresh
        assertEquals(initialRefreshCount, fakeRepository.refreshCount)
    }

    @Test
    fun `error state set when refresh fails and no cached data`() {
        fakeRepository.shouldThrowOnRefresh = true
        viewModel = createViewModel()

        assertEquals("Network error", viewModel.uiState.value.errorMessage)
    }

    // --- Fake ---

    private class FakeAlertsRepository : AlertsRepository {
        val alertsFlow = MutableStateFlow<List<ScamAlert>>(emptyList())
        var defaultFilter = AlertRegionFilter.ALL
        var shouldThrowOnRefresh = false
        var refreshCount = 0

        override fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>> = alertsFlow
        override suspend fun refreshAlerts(filter: AlertRegionFilter) {
            refreshCount++
            if (shouldThrowOnRefresh) throw RuntimeException("Network error")
        }
        override suspend fun getAlertById(id: String): ScamAlert? = null
        override fun getDefaultRegionFilter(): AlertRegionFilter = defaultFilter
    }
}
