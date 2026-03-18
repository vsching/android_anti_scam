package com.safeanot.app.feature.profile

import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.usecase.AuditStats
import com.safeanot.app.domain.usecase.GetAuditStatsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `GetAuditStatsUseCase combines repository flows correctly`() = kotlinx.coroutines.test.runTest {
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
    fun `GetAuditStatsUseCase defaults to zero score when null`() = kotlinx.coroutines.test.runTest {
        val fakeRepo = FakeAuditRepository()
        fakeRepo.securityScoreFlow.value = null

        val useCase = GetAuditStatsUseCase(fakeRepo)
        val result = useCase().first()

        assertEquals(0, result.totalAudits)
        assertNull(result.lastAuditTimestamp)
        assertEquals(0, result.securityScore)
    }

    // --- Fake ---

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
}
