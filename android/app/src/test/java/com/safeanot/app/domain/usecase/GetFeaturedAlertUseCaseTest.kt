package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AlertRegion
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetFeaturedAlertUseCaseTest {

    private lateinit var fakeAlertsRepository: FakeAlertsRepository
    private lateinit var fakePrefsRepository: FakePrefsRepository
    private lateinit var useCase: GetFeaturedAlertUseCase

    @Before
    fun setup() {
        fakeAlertsRepository = FakeAlertsRepository()
        fakePrefsRepository = FakePrefsRepository()
        useCase = GetFeaturedAlertUseCase(fakeAlertsRepository, fakePrefsRepository)
    }

    @Test
    fun `returns first high severity alert`() = runTest {
        val highAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH)
        val lowAlert = makeAlert(id = "2", severity = AlertSeverity.LOW)
        fakeAlertsRepository.alertsFlow.value = listOf(highAlert, lowAlert)

        val result = useCase().first()

        assertEquals(highAlert, result)
    }

    @Test
    fun `returns null when no high severity alerts`() = runTest {
        val lowAlert = makeAlert(id = "1", severity = AlertSeverity.LOW)
        val medAlert = makeAlert(id = "2", severity = AlertSeverity.MEDIUM)
        fakeAlertsRepository.alertsFlow.value = listOf(lowAlert, medAlert)

        val result = useCase().first()

        assertNull(result)
    }

    @Test
    fun `returns null when alert list is empty`() = runTest {
        fakeAlertsRepository.alertsFlow.value = emptyList()

        val result = useCase().first()

        assertNull(result)
    }

    @Test
    fun `returns first high severity by list order`() = runTest {
        val high1 = makeAlert(id = "1", severity = AlertSeverity.HIGH)
        val high2 = makeAlert(id = "2", severity = AlertSeverity.HIGH)
        fakeAlertsRepository.alertsFlow.value = listOf(high1, high2)

        val result = useCase().first()

        assertEquals("1", result?.id)
    }

    @Test
    fun `filters by user region preference - Malaysia`() = runTest {
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.MALAYSIA
        val myAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH, region = AlertRegion.MY)
        val sgAlert = makeAlert(id = "2", severity = AlertSeverity.HIGH, region = AlertRegion.SG)
        fakeAlertsRepository.alertsFlow.value = listOf(sgAlert, myAlert)

        val result = useCase().first()

        assertEquals("1", result?.id)
    }

    @Test
    fun `BOTH region alerts match any user preference`() = runTest {
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.SINGAPORE
        val bothAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH, region = AlertRegion.BOTH)
        fakeAlertsRepository.alertsFlow.value = listOf(bothAlert)

        val result = useCase().first()

        assertEquals("1", result?.id)
    }

    @Test
    fun `ALL region preference shows all alerts`() = runTest {
        fakePrefsRepository.regionFlow.value = AlertRegionFilter.ALL
        val sgAlert = makeAlert(id = "1", severity = AlertSeverity.HIGH, region = AlertRegion.SG)
        fakeAlertsRepository.alertsFlow.value = listOf(sgAlert)

        val result = useCase().first()

        assertEquals("1", result?.id)
    }

    private fun makeAlert(
        id: String = "1",
        severity: AlertSeverity = AlertSeverity.HIGH,
        region: AlertRegion = AlertRegion.MY,
    ) = ScamAlert(
        id = id,
        title = "Test Alert $id",
        description = "Description",
        scamType = "phishing",
        severity = severity,
        region = region,
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

    private class FakePrefsRepository : UserPreferencesRepository {
        val regionFlow = MutableStateFlow(AlertRegionFilter.ALL)
        override fun getRegion(): Flow<AlertRegionFilter> = regionFlow
        override suspend fun setRegion(region: AlertRegionFilter) { regionFlow.value = region }
        override fun getScamAlertsEnabled(): Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setScamAlertsEnabled(enabled: Boolean) {}
    }
}
