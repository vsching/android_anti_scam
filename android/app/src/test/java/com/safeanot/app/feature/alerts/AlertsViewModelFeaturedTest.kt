package com.safeanot.app.feature.alerts

import com.safeanot.app.domain.model.AlertRegion
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.repository.UserPreferencesRepository
import com.safeanot.app.domain.usecase.GetFeaturedAlertUseCase
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase
import com.safeanot.app.domain.usecase.ObserveAlertsUseCase
import com.safeanot.app.domain.usecase.RefreshAlertsUseCase
import com.safeanot.app.testutil.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelFeaturedTest {

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
            getFeaturedAlertUseCase = GetFeaturedAlertUseCase(
                fakeRepository,
                fakePrefsRepository,
            ),
        )
    }

    @Test
    fun `featured alert populated when high severity alert exists`() {
        val highAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH)
        fakeRepository.alertsFlow.value = listOf(highAlert)

        val viewModel = createViewModel()

        assertNotNull(viewModel.uiState.value.featuredAlert)
        assertEquals("1", viewModel.uiState.value.featuredAlert?.id)
    }

    @Test
    fun `featured alert is null when no high severity alerts`() {
        val lowAlert = makeAlert(id = "1", severity = AlertSeverity.LOW)
        fakeRepository.alertsFlow.value = listOf(lowAlert)

        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.featuredAlert)
    }

    @Test
    fun `dismiss hides featured alert`() {
        val highAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH)
        fakeRepository.alertsFlow.value = listOf(highAlert)

        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isFeaturedDismissed)

        viewModel.onDismissFeatured()

        assertTrue(viewModel.uiState.value.isFeaturedDismissed)
    }

    @Test
    fun `dismissed state is session-only - new ViewModel instance resets it`() {
        val highAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH)
        fakeRepository.alertsFlow.value = listOf(highAlert)

        val viewModel1 = createViewModel()
        viewModel1.onDismissFeatured()
        assertTrue(viewModel1.uiState.value.isFeaturedDismissed)

        // Simulates app restart / new ViewModel
        val viewModel2 = createViewModel()
        assertFalse(viewModel2.uiState.value.isFeaturedDismissed)
        assertNotNull(viewModel2.uiState.value.featuredAlert)
    }

    @Test
    fun `featured alert updates when alerts change`() {
        fakeRepository.alertsFlow.value = emptyList()
        val viewModel = createViewModel()
        assertNull(viewModel.uiState.value.featuredAlert)

        // Add a high severity alert
        val highAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH)
        fakeRepository.alertsFlow.value = listOf(highAlert)

        assertNotNull(viewModel.uiState.value.featuredAlert)
        assertEquals("1", viewModel.uiState.value.featuredAlert?.id)
    }

    private fun makeAlert(
        id: String = "1",
        severity: AlertSeverity = AlertSeverity.HIGH,
    ) = ScamAlert(
        id = id,
        title = "Test Alert $id",
        description = "Description",
        scamType = "phishing",
        severity = severity,
        region = AlertRegion.MY,
        reportCount = 10,
        createdAt = "2026-03-15T10:00:00",
        createdAtEpochMs = 1773750000000L,
    )

    private class FakeAlertsRepository : AlertsRepository {
        val alertsFlow = MutableStateFlow<List<ScamAlert>>(emptyList())
        override fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>> = alertsFlow
        override suspend fun refreshAlerts(filter: AlertRegionFilter) {}
        override suspend fun getAlertById(id: String): ScamAlert? = null
        override fun getDefaultRegionFilter(): AlertRegionFilter = AlertRegionFilter.ALL
    }
}
