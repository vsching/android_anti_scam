package com.safeanot.app.feature.profile

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.UserPreferencesRepository
import com.safeanot.app.domain.usecase.GetAuditStatsUseCase
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase
import com.safeanot.app.domain.usecase.SetPreferredRegionUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- formatLastAuditDate tests ---

    @Test
    fun `formatLastAuditDate returns null for null timestamp`() {
        assertNull(ProfileViewModel.formatLastAuditDate(null))
    }

    @Test
    fun `formatLastAuditDate formats just now`() {
        val now = System.currentTimeMillis()
        assertEquals("Last audit: just now", ProfileViewModel.formatLastAuditDate(now))
    }

    @Test
    fun `formatLastAuditDate formats minutes ago`() {
        val thirtyMinAgo = System.currentTimeMillis() - (30 * 60 * 1000)
        assertEquals("Last audit: 30m ago", ProfileViewModel.formatLastAuditDate(thirtyMinAgo))
    }

    @Test
    fun `formatLastAuditDate formats hours ago`() {
        val fiveHoursAgo = System.currentTimeMillis() - (5L * 60 * 60 * 1000)
        assertEquals("Last audit: 5h ago", ProfileViewModel.formatLastAuditDate(fiveHoursAgo))
    }

    @Test
    fun `formatLastAuditDate formats 1 day ago`() {
        val oneDayAgo = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
        assertEquals("Last audit: 1 day ago", ProfileViewModel.formatLastAuditDate(oneDayAgo))
    }

    @Test
    fun `formatLastAuditDate formats multiple days ago`() {
        val threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000)
        assertEquals("Last audit: 3 days ago", ProfileViewModel.formatLastAuditDate(threeDaysAgo))
    }

    // --- GetAuditStatsUseCase tests ---

    @Test
    fun `GetAuditStatsUseCase combines repository flows correctly`() = runTest {
        val fakeRepo = FakeAuditRepository()
        fakeRepo.auditCountFlow.value = 5
        fakeRepo.lastAuditTimestampFlow.value = 1000L
        fakeRepo.securityScoreFlow.value = SecurityScore(scorePercent = 80)

        val useCase = GetAuditStatsUseCase(fakeRepo)
        val result = useCase().first()

        assertEquals(5, result.totalAudits)
        assertEquals(1000L, result.lastAuditTimestamp)
        assertEquals(80, result.securityScore)
    }

    @Test
    fun `GetAuditStatsUseCase defaults to zero score when null`() = runTest {
        val fakeRepo = FakeAuditRepository()
        fakeRepo.securityScoreFlow.value = null

        val useCase = GetAuditStatsUseCase(fakeRepo)
        val result = useCase().first()

        assertEquals(0, result.totalAudits)
        assertNull(result.lastAuditTimestamp)
        assertEquals(0, result.securityScore)
    }

    // --- Region preference use case tests ---

    @Test
    fun `GetPreferredRegionUseCase returns region from repository`() = runTest {
        val fakePrefs = FakeUserPreferencesRepository()
        fakePrefs.regionFlow.value = AlertRegionFilter.MALAYSIA
        val useCase = GetPreferredRegionUseCase(fakePrefs)

        val result = useCase().first()

        assertEquals(AlertRegionFilter.MALAYSIA, result)
    }

    @Test
    fun `SetPreferredRegionUseCase persists region via repository`() = runTest {
        val fakePrefs = FakeUserPreferencesRepository()
        val useCase = SetPreferredRegionUseCase(fakePrefs)

        useCase(AlertRegionFilter.SINGAPORE)

        assertEquals(AlertRegionFilter.SINGAPORE, fakePrefs.regionFlow.value)
    }

    @Test
    fun `region change propagates through GetPreferredRegionUseCase`() = runTest {
        val fakePrefs = FakeUserPreferencesRepository()
        val getUseCase = GetPreferredRegionUseCase(fakePrefs)
        val setUseCase = SetPreferredRegionUseCase(fakePrefs)

        assertEquals(AlertRegionFilter.ALL, getUseCase().first())

        setUseCase(AlertRegionFilter.MALAYSIA)
        assertEquals(AlertRegionFilter.MALAYSIA, getUseCase().first())
    }

    // --- Scam alerts toggle tests ---

    @Test
    fun `scam alerts disabled via repository persists false`() = runTest {
        val fakePrefs = FakeUserPreferencesRepository()

        fakePrefs.setScamAlertsEnabled(false)

        assertFalse(fakePrefs.getScamAlertsEnabled().first())
    }

    @Test
    fun `scam alerts toggle back to true after being disabled`() = runTest {
        val fakePrefs = FakeUserPreferencesRepository()
        fakePrefs.setScamAlertsEnabled(false)
        fakePrefs.setScamAlertsEnabled(true)

        assertTrue(fakePrefs.getScamAlertsEnabled().first())
    }

    // --- Fakes ---

    private class FakeAuditRepository : AuditRepository {
        val auditCountFlow = MutableStateFlow(0)
        val lastAuditTimestampFlow = MutableStateFlow<Long?>(null)
        val securityScoreFlow = MutableStateFlow<SecurityScore?>(null)

        override fun getAllAuditItems(): Flow<List<AuditItem>> = MutableStateFlow(emptyList())
        override fun getAuditItemsByCategory(category: String): Flow<List<AuditItem>> =
            MutableStateFlow(emptyList())
        override fun getSecurityScore(): Flow<SecurityScore?> = securityScoreFlow
        override suspend fun runAudit() {}
        override suspend fun runAuditAndDetectChanges(): AuditChangeSummary =
            AuditChangeSummary(emptyList(), emptyList())
        override suspend fun updateItemStatus(id: Int, status: AuditStatus) {}
        override suspend fun updateItemStatusByPackage(packageName: String, status: AuditStatus) {}
        override suspend fun recalculateScore() {}
        override fun getCompletedAuditCount(): Flow<Int> = auditCountFlow
        override fun getLastAuditTimestamp(): Flow<Long?> = lastAuditTimestampFlow
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
