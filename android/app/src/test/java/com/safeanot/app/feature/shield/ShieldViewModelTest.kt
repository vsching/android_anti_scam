package com.safeanot.app.feature.shield

import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.CardFormat
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.ShareEventRepository
import com.safeanot.app.domain.usecase.GenerateScoreCardUseCase
import com.safeanot.app.domain.usecase.GetSecurityScoreUseCase
import com.safeanot.app.domain.usecase.RunAuditUseCase
import com.safeanot.app.domain.usecase.TrackShareEventUseCase
import com.safeanot.app.feature.check.ShareEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShieldViewModelTest {

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
    fun `shareScore emits BitmapOnly event`() = runTest {
        val vm = createViewModel()

        val event = async { vm.shareEvents.first() }
        vm.shareScore(CardFormat.SQUARE)

        val result = event.await()
        assertTrue("Expected BitmapOnly but got $result", result is ShareEvent.BitmapOnly)
        assertNotNull((result as ShareEvent.BitmapOnly).bitmap)
    }

    @Test
    fun `shareScore with VERTICAL format emits BitmapOnly event`() = runTest {
        val vm = createViewModel()

        val event = async { vm.shareEvents.first() }
        vm.shareScore(CardFormat.VERTICAL)

        val result = event.await()
        assertTrue(result is ShareEvent.BitmapOnly)
    }

    @Test
    fun `constructor has no Context parameter`() {
        // Verifies that ShieldViewModel can be constructed without Context.
        // The constructor takes only use cases and repository — if Context were
        // required, this test would fail to compile.
        val vm = createViewModel()
        assertNotNull(vm)
    }

    @Test
    fun `onShareCompleted tracks share event`() = runTest {
        val fakeShareRepo = FakeShareEventRepository()
        val vm = createViewModel(shareRepo = fakeShareRepo)

        vm.onShareCompleted(ShareType.SCORE, "security_score", SharePlatform.GENERIC)

        assertEquals(1, fakeShareRepo.trackedEvents.size)
        val event = fakeShareRepo.trackedEvents.first()
        assertEquals(ShareType.SCORE, event.shareType)
        assertEquals("security_score", event.contentId)
        assertEquals(SharePlatform.GENERIC, event.platform)
    }

    private fun createViewModel(
        shareRepo: ShareEventRepository = FakeShareEventRepository(),
    ): ShieldViewModel {
        val fakeRepo = FakeAuditRepository()
        return ShieldViewModel(
            runAuditUseCase = RunAuditUseCase(fakeRepo),
            getSecurityScoreUseCase = GetSecurityScoreUseCase(fakeRepo),
            generateScoreCardUseCase = GenerateScoreCardUseCase(),
            repository = fakeRepo,
            trackShareEventUseCase = TrackShareEventUseCase(shareRepo),
        )
    }

    /** Fake share event repository for testing. */
    private class FakeShareEventRepository : ShareEventRepository {
        val trackedEvents = mutableListOf<ShareEventModel>()
        override suspend fun trackEvent(event: ShareEventModel): Boolean {
            trackedEvents.add(event)
            return true
        }
        override suspend fun syncPendingEvents(): Boolean = true
    }

    /** Fake audit repository for testing. */
    private class FakeAuditRepository : AuditRepository {
        override fun getAllAuditItems(): Flow<List<AuditItem>> = flowOf(emptyList())
        override fun getAuditItemsByCategory(category: String): Flow<List<AuditItem>> =
            flowOf(emptyList())
        override fun getSecurityScore(): Flow<SecurityScore?> =
            flowOf(SecurityScore(totalItems = 10, securedItems = 8, scorePercent = 80))
        override suspend fun runAudit() {}
        override suspend fun runAuditAndDetectChanges(): AuditChangeSummary =
            AuditChangeSummary()
        override suspend fun updateItemStatus(id: Int, status: AuditStatus) {}
        override suspend fun updateItemStatusByPackage(packageName: String, status: AuditStatus) {}
        override suspend fun recalculateScore() {}
        override fun getCompletedAuditCount(): Flow<Int> = flowOf(0)
        override fun getLastAuditTimestamp(): Flow<Long?> = flowOf(null)
    }
}
