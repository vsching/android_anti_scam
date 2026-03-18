package com.safeanot.app.feature.profile

import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.EmergencyContacts
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.usecase.GetAuditStatsUseCase
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase
import com.safeanot.app.domain.usecase.SetPreferredRegionUseCase
import com.safeanot.app.testutil.FakeUserPreferencesRepository
import com.safeanot.app.testutil.TestProfileViewModelFactory
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
    private lateinit var fakeAuditRepo: FakeAuditRepository
    private lateinit var fakePrefs: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAuditRepo = FakeAuditRepository()
        fakePrefs = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel {
        return TestProfileViewModelFactory.create(
            fakeAuditRepo = fakeAuditRepo,
            fakePrefs = fakePrefs,
        )
    }

    // --- ViewModel integration tests ---

    @Test
    fun `initial uiState has correct defaults`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.remindersEnabled)
        assertEquals(7, state.reminderIntervalDays)
        assertEquals(0, state.totalAudits)
        assertEquals(0, state.securityScore)
        assertNull(state.lastAuditDate)
        assertEquals(AlertRegionFilter.ALL, state.selectedRegion)
        assertTrue(state.scamAlertsEnabled)
        assertTrue(state.emergencyContacts.isEmpty())
    }

    @Test
    fun `setRegion updates selectedRegion and emergencyContacts via ViewModel`() = runTest {
        val viewModel = createViewModel()

        viewModel.setRegion(AlertRegionFilter.MALAYSIA)

        val state = viewModel.uiState.value
        assertEquals(AlertRegionFilter.MALAYSIA, state.selectedRegion)
        assertEquals(EmergencyContacts.forRegion(AlertRegionFilter.MALAYSIA), state.emergencyContacts)
    }

    @Test
    fun `toggleScamAlerts updates scamAlertsEnabled via ViewModel`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleScamAlerts(false)

        assertFalse(viewModel.uiState.value.scamAlertsEnabled)
    }

    @Test
    fun `toggleScamAlerts back to true after being disabled via ViewModel`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleScamAlerts(false)
        viewModel.toggleScamAlerts(true)

        assertTrue(viewModel.uiState.value.scamAlertsEnabled)
    }

    @Test
    fun `shareApp emits event via ViewModel`() = runTest {
        val viewModel = createViewModel()

        viewModel.shareApp()

        val event = viewModel.shareEvent.first()
        assertEquals(Unit, event)
    }

    @Test
    fun `audit stats flow updates ViewModel state`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 5
        fakeAuditRepo.lastAuditTimestampFlow.value = 1000L
        fakeAuditRepo.securityScoreFlow.value = SecurityScore(scorePercent = 80)

        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(5, state.totalAudits)
        assertEquals(80, state.securityScore)
    }

    @Test
    fun `region change updates emergency contacts via ViewModel`() = runTest {
        val viewModel = createViewModel()

        viewModel.setRegion(AlertRegionFilter.SINGAPORE)

        val contacts = viewModel.uiState.value.emergencyContacts
        assertEquals(3, contacts.size)
        assertTrue(contacts.any { it.name == "ScamShield" })
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
        fakeAuditRepo.auditCountFlow.value = 5
        fakeAuditRepo.lastAuditTimestampFlow.value = 1000L
        fakeAuditRepo.securityScoreFlow.value = SecurityScore(scorePercent = 80)

        val useCase = GetAuditStatsUseCase(fakeAuditRepo)
        val result = useCase().first()

        assertEquals(5, result.totalAudits)
        assertEquals(1000L, result.lastAuditTimestamp)
        assertEquals(80, result.securityScore)
    }

    @Test
    fun `GetAuditStatsUseCase defaults to zero score when null`() = runTest {
        fakeAuditRepo.securityScoreFlow.value = null

        val useCase = GetAuditStatsUseCase(fakeAuditRepo)
        val result = useCase().first()

        assertEquals(0, result.totalAudits)
        assertNull(result.lastAuditTimestamp)
        assertEquals(0, result.securityScore)
    }

    // --- Legal links tests ---

    @Test
    fun `legal links are non-empty in default state`() {
        val state = ProfileUiState()
        assertTrue(state.legalLinks.isNotEmpty())
        assertTrue(state.legalLinks.containsKey("Privacy Policy"))
        assertTrue(state.legalLinks.containsKey("Terms of Service"))
    }

    @Test
    fun `legal links contain valid URLs`() {
        val state = ProfileUiState()
        state.legalLinks.values.forEach { url ->
            assertTrue("URL '$url' should start with https://", url.startsWith("https://"))
        }
    }

    // --- Fakes ---

    internal class FakeAuditRepository : AuditRepository {
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
}
