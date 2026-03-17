package com.safeanot.app.feature.alerts

import androidx.lifecycle.SavedStateHandle
import com.safeanot.app.domain.model.AlertRegion
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.usecase.GetAlertByIdUseCase
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.repository.AlertsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads alert by ID on init`() {
        val alert = createTestAlert("test-1")
        val repo = FakeRepository(alert)
        val vm = AlertDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("alertId" to "test-1")),
            getAlertByIdUseCase = GetAlertByIdUseCase(repo),
        )

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.alert)
        assertEquals("test-1", vm.uiState.value.alert!!.id)
    }

    @Test
    fun `generates share text`() {
        val alert = createTestAlert("test-2")
        val repo = FakeRepository(alert)
        val vm = AlertDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("alertId" to "test-2")),
            getAlertByIdUseCase = GetAlertByIdUseCase(repo),
        )

        assertNotNull(vm.uiState.value.shareText)
        assertTrue(vm.uiState.value.shareText!!.contains("test-2"))
    }

    @Test
    fun `generates safety tips`() {
        val alert = createTestAlert("test-3")
        val repo = FakeRepository(alert)
        val vm = AlertDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("alertId" to "test-3")),
            getAlertByIdUseCase = GetAlertByIdUseCase(repo),
        )

        assertTrue(vm.uiState.value.safetyTips.isNotEmpty())
    }

    @Test
    fun `shows error when alert not found`() {
        val repo = FakeRepository(null)
        val vm = AlertDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("alertId" to "missing")),
            getAlertByIdUseCase = GetAlertByIdUseCase(repo),
        )

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Alert not found", vm.uiState.value.errorMessage)
    }

    private fun createTestAlert(id: String) = ScamAlert(
        id = id,
        title = "Test Alert",
        description = "Test description",
        scamType = "phishing",
        severity = AlertSeverity.HIGH,
        region = AlertRegion.MY,
        reportCount = 10,
        createdAt = "2026-03-15",
        createdAtEpochMs = 0L,
    )

    private class FakeRepository(private val alertToReturn: ScamAlert?) : AlertsRepository {
        override fun observeAlerts(filter: AlertRegionFilter): Flow<List<ScamAlert>> = emptyFlow()
        override suspend fun refreshAlerts(filter: AlertRegionFilter) {}
        override suspend fun getAlertById(id: String): ScamAlert? = alertToReturn
        override fun getDefaultRegionFilter(): AlertRegionFilter = AlertRegionFilter.ALL
    }
}
