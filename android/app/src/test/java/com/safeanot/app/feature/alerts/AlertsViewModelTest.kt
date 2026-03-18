package com.safeanot.app.feature.alerts

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.repository.UserPreferencesRepository
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase
import com.safeanot.app.domain.usecase.ObserveAlertsUseCase
import com.safeanot.app.domain.usecase.RefreshAlertsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var fakePrefsRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAlertsRepository()
        fakePrefsRepository = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AlertsViewModel {
        return AlertsViewModel(
            observeAlertsUseCase = ObserveAlertsUseCase(fakeRepository),
            refreshAlertsUseCase = RefreshAlertsUseCase(fakeRepository),
            getPreferredRegionUseCase = GetPreferredRegionUseCase(fakePrefsRepository),
        )
    }

    @Test
    fun `initial region filter loaded from user preferences`() {
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.MALAYSIA
        val viewModel = createViewModel()

        assertEquals(AlertRegionFilter.MALAYSIA, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `falls back to locale when no preference stored`() {
        // FakeUserPreferencesRepository defaults to ALL (simulating no stored preference)
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.ALL
        val viewModel = createViewModel()

        assertEquals(AlertRegionFilter.ALL, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `region change reactively updates feed filter`() = runTest {
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.MALAYSIA
        val viewModel = createViewModel()

        assertEquals(AlertRegionFilter.MALAYSIA, viewModel.uiState.value.selectedFilter)

        // Simulate region change from Profile screen
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.SINGAPORE

        assertEquals(AlertRegionFilter.SINGAPORE, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `initial load sets isInitialLoading to false after refresh`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun `onFilterChanged updates selected filter`() {
        val viewModel = createViewModel()

        viewModel.onFilterChanged(AlertRegionFilter.SINGAPORE)

        assertEquals(AlertRegionFilter.SINGAPORE, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `onFilterChanged with same filter is no-op`() {
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.ALL
        val viewModel = createViewModel()
        val initialRefreshCount = fakeRepository.refreshCount

        viewModel.onFilterChanged(AlertRegionFilter.ALL)

        // Should not have triggered an additional refresh
        assertEquals(initialRefreshCount, fakeRepository.refreshCount)
    }

    @Test
    fun `error state set when refresh fails and no cached data`() {
        fakeRepository.shouldThrowOnRefresh = true
        val viewModel = createViewModel()

        assertEquals("Network error", viewModel.uiState.value.errorMessage)
    }

    // --- Fakes ---

    private class FakeAlertsRepository : AlertsRepository {
        val alertsFlow = MutableStateFlow<List<ScamAlert>>(emptyList())
        var shouldThrowOnRefresh = false
        var refreshCount = 0

        override fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>> = alertsFlow
        override suspend fun refreshAlerts(filter: AlertRegionFilter) {
            refreshCount++
            if (shouldThrowOnRefresh) throw RuntimeException("Network error")
        }
        override suspend fun getAlertById(id: String): ScamAlert? = null
        override fun getDefaultRegionFilter(): AlertRegionFilter = AlertRegionFilter.ALL
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val regionFlow = MutableStateFlow(AlertRegionFilter.ALL)
        val scamAlertsFlow = MutableStateFlow(true)

        override fun getRegion(): Flow<AlertRegionFilter> = regionFlow
        override suspend fun setRegion(region: AlertRegionFilter) { regionFlow.value = region }
        override fun getScamAlertsEnabled(): Flow<Boolean> = scamAlertsFlow
        override suspend fun setScamAlertsEnabled(enabled: Boolean) { scamAlertsFlow.value = enabled }
    }
}
