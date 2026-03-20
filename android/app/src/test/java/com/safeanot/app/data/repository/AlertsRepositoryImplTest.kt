package com.safeanot.app.data.repository

import com.safeanot.app.data.local.AlertsDao
import com.safeanot.app.data.local.entity.AlertEntity
import com.safeanot.app.data.remote.SafeAnotApi
import com.safeanot.app.data.remote.model.AlertDto
import com.safeanot.app.data.remote.model.ClaimPairingCodeRequest
import com.safeanot.app.data.remote.model.DeletePairingRequest
import com.safeanot.app.data.remote.model.GeneratePairingCodeRequest
import com.safeanot.app.data.remote.model.GuardianPairingDto
import com.safeanot.app.data.remote.model.PairingCodeResponse
import retrofit2.Response
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AlertSeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class AlertsRepositoryImplTest {

    private lateinit var fakeApi: FakeSafeAnotApi
    private lateinit var fakeDao: FakeAlertsDao
    private lateinit var repository: AlertsRepositoryImpl

    @Before
    fun setup() {
        fakeApi = FakeSafeAnotApi()
        fakeDao = FakeAlertsDao()
        repository = AlertsRepositoryImpl(fakeApi, fakeDao)
    }

    @Test
    fun `refreshAlerts fetches from API and caches in DAO`() = runTest {
        val dto = AlertDto(
            id = "1",
            title = "Test Alert",
            description = "Test description",
            scamType = "phishing",
            severity = "HIGH",
            region = "MY",
            reportCount = 10,
            createdAt = "2026-03-15",
        )
        fakeApi.alertsToReturn = listOf(dto)

        repository.refreshAlerts(AlertRegionFilter.ALL)

        assertEquals(1, fakeDao.allEntities.size)
        assertEquals("Test Alert", fakeDao.allEntities[0].title)
    }

    @Test
    fun `refreshAlerts with region filter passes region to API`() = runTest {
        fakeApi.alertsToReturn = emptyList()

        repository.refreshAlerts(AlertRegionFilter.MALAYSIA)

        assertEquals("MY", fakeApi.lastRegionQuery)
    }

    @Test
    fun `refreshAlerts with ALL passes null region to API`() = runTest {
        fakeApi.alertsToReturn = emptyList()

        repository.refreshAlerts(AlertRegionFilter.ALL)

        assertNull(fakeApi.lastRegionQuery)
    }

    @Test
    fun `getAlertById returns domain model when found`() = runTest {
        fakeDao.allEntities.add(
            AlertEntity(
                id = "abc",
                title = "Found",
                description = "desc",
                scamType = "malware",
                severity = "MEDIUM",
                region = "SG",
                reportCount = 5,
                createdAt = "2026-03-10",
            )
        )

        val result = repository.getAlertById("abc")

        assertNotNull(result)
        assertEquals("Found", result!!.title)
        assertEquals(AlertSeverity.MEDIUM, result.severity)
    }

    @Test
    fun `getAlertById returns null when not found`() = runTest {
        val result = repository.getAlertById("nonexistent")
        assertNull(result)
    }

    @Test
    fun `observeAlerts returns mapped domain models`() = runTest {
        fakeDao.allEntities.add(
            AlertEntity(
                id = "1",
                title = "Alert 1",
                description = "desc",
                scamType = "phishing",
                severity = "HIGH",
                region = "MY",
                reportCount = 3,
                createdAt = "2026-03-15",
            )
        )
        fakeDao.emitUpdate()

        val alerts = repository.observeAlerts(AlertRegionFilter.ALL).first()

        assertEquals(1, alerts.size)
        assertEquals("Alert 1", alerts[0].title)
        assertEquals(AlertSeverity.HIGH, alerts[0].severity)
    }

    @Test
    fun `getDefaultRegionFilter returns ALL for unknown locale`() {
        Locale.setDefault(Locale("en", "US"))
        val repo = AlertsRepositoryImpl(fakeApi, fakeDao)
        assertEquals(AlertRegionFilter.ALL, repo.getDefaultRegionFilter())
    }

    @Test
    fun `getDefaultRegionFilter returns MALAYSIA for MY locale`() {
        Locale.setDefault(Locale("ms", "MY"))
        val repo = AlertsRepositoryImpl(fakeApi, fakeDao)
        assertEquals(AlertRegionFilter.MALAYSIA, repo.getDefaultRegionFilter())
    }

    @Test
    fun `getDefaultRegionFilter returns SINGAPORE for SG locale`() {
        Locale.setDefault(Locale("en", "SG"))
        val repo = AlertsRepositoryImpl(fakeApi, fakeDao)
        assertEquals(AlertRegionFilter.SINGAPORE, repo.getDefaultRegionFilter())
    }

    // --- Fakes ---

    private class FakeSafeAnotApi : SafeAnotApi {
        var alertsToReturn: List<AlertDto> = emptyList()
        var lastRegionQuery: String? = "UNSET"

        override suspend fun getAlerts(region: String?): List<AlertDto> {
            lastRegionQuery = region
            return alertsToReturn
        }

        // Unused stubs
        override suspend fun getLatestMetadata() = throw NotImplementedError()
        override suspend fun getFullDatabase() = throw NotImplementedError()
        override suspend fun getDelta(since: String) = throw NotImplementedError()
        override suspend fun getBloomFilter() = throw NotImplementedError()
        override suspend fun checkDomain(request: com.safeanot.app.data.remote.model.CheckRequest) =
            throw NotImplementedError()
        override suspend fun postShareEvents(request: com.safeanot.app.data.remote.model.ShareEventBatchRequest) =
            throw NotImplementedError()
        override suspend fun generatePairingCode(request: GeneratePairingCodeRequest): PairingCodeResponse = throw NotImplementedError()
        override suspend fun claimPairingCode(request: ClaimPairingCodeRequest): GuardianPairingDto = throw NotImplementedError()
        override suspend fun getWards(deviceId: String): List<GuardianPairingDto> = throw NotImplementedError()
        override suspend fun getGuardians(deviceId: String): List<GuardianPairingDto> = throw NotImplementedError()
        override suspend fun deletePairing(request: DeletePairingRequest): Response<Unit> = throw NotImplementedError()
    }

    private class FakeAlertsDao : AlertsDao {
        val allEntities = mutableListOf<AlertEntity>()
        private val allFlow = MutableStateFlow<List<AlertEntity>>(emptyList())
        private val regionFlow = MutableStateFlow<List<AlertEntity>>(emptyList())

        fun emitUpdate() {
            allFlow.value = allEntities.toList()
            regionFlow.value = allEntities.toList()
        }

        override fun observeAlertsAll(): Flow<List<AlertEntity>> = allFlow
        override fun observeAlertsByRegion(region: String): Flow<List<AlertEntity>> = regionFlow
        override suspend fun getAlertById(id: String) = allEntities.find { it.id == id }
        override suspend fun insertAll(alerts: List<AlertEntity>) {
            allEntities.addAll(alerts)
            emitUpdate()
        }
        override suspend fun clearAll() {
            allEntities.clear()
            emitUpdate()
        }
        override suspend fun clearByRegion(region: String) {
            allEntities.removeAll { it.region == region || it.region == "both" }
            emitUpdate()
        }
        override suspend fun replaceAll(alerts: List<AlertEntity>) {
            clearAll()
            insertAll(alerts)
        }
        override suspend fun replaceByRegion(region: String, alerts: List<AlertEntity>) {
            clearByRegion(region)
            insertAll(alerts)
        }
    }
}
