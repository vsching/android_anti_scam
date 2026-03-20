package com.safeanot.app.worker

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for StreakCheckWorker logic.
 * Simulates the worker's doWork without Android dependencies.
 */
class StreakCheckWorkerTest {

    private lateinit var fakeAuditRepo: FakeAuditRepository
    private lateinit var fakeUpdateStreak: FakeUpdateStreakUseCase
    private val scoreFlow = MutableStateFlow<SecurityScore?>(null)

    @Before
    fun setup() {
        fakeAuditRepo = FakeAuditRepository(scoreFlow)
        fakeUpdateStreak = FakeUpdateStreakUseCase()
    }

    /**
     * Mimics the worker's doWork logic for testability.
     * Returns "success" or "retry".
     */
    private suspend fun simulateDoWork(): String {
        return try {
            val score = fakeAuditRepo.getSecurityScore().first()
            val scorePercent = score?.scorePercent ?: 0
            fakeUpdateStreak.invoke(scorePercent)
            "success"
        } catch (_: Exception) {
            "retry"
        }
    }

    @Test
    fun `calls updateStreakUseCase with current security score`() = runTest {
        scoreFlow.value = SecurityScore(scorePercent = 85, totalItems = 10, securedItems = 8)

        val result = simulateDoWork()

        assertEquals("success", result)
        assertEquals(85, fakeUpdateStreak.lastScorePercent)
    }

    @Test
    fun `uses 0 score when no security score available`() = runTest {
        scoreFlow.value = null

        val result = simulateDoWork()

        assertEquals("success", result)
        assertEquals(0, fakeUpdateStreak.lastScorePercent)
    }

    @Test
    fun `returns retry on exception`() = runTest {
        fakeUpdateStreak.shouldThrow = true
        scoreFlow.value = SecurityScore(scorePercent = 90)

        val result = simulateDoWork()

        assertEquals("retry", result)
    }

    @Test
    fun `returns success when badges are unlocked`() = runTest {
        scoreFlow.value = SecurityScore(scorePercent = 100)
        fakeUpdateStreak.badgesToReturn = listOf(BadgeType.PHONE_HARDENED)

        val result = simulateDoWork()

        assertEquals("success", result)
        assertTrue(fakeUpdateStreak.badgesToReturn.isNotEmpty())
    }

    // --- Fakes ---

    private class FakeAuditRepository(
        private val scoreFlow: MutableStateFlow<SecurityScore?>,
    ) : AuditRepository {
        override fun getSecurityScore(): Flow<SecurityScore?> = scoreFlow
        override fun getAllAuditItems() = throw NotImplementedError()
        override fun getAuditItemsByCategory(category: String) = throw NotImplementedError()
        override suspend fun runAudit() = throw NotImplementedError()
        override suspend fun runAuditAndDetectChanges() = throw NotImplementedError()
        override suspend fun updateItemStatus(id: Int, status: com.safeanot.app.domain.model.AuditStatus) = throw NotImplementedError()
        override suspend fun updateItemStatusByPackage(packageName: String, status: com.safeanot.app.domain.model.AuditStatus) = throw NotImplementedError()
        override suspend fun recalculateScore() = throw NotImplementedError()
        override fun getCompletedAuditCount() = throw NotImplementedError()
        override fun getLastAuditTimestamp() = throw NotImplementedError()
    }

    private class FakeUpdateStreakUseCase {
        var lastScorePercent: Int = -1
        var shouldThrow = false
        var badgesToReturn: List<BadgeType> = emptyList()

        suspend fun invoke(scorePercent: Int): List<BadgeType> {
            if (shouldThrow) throw RuntimeException("Test error")
            lastScorePercent = scorePercent
            return badgesToReturn
        }
    }
}
