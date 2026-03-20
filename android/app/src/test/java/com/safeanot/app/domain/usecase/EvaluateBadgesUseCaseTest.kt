package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.BadgeRepository
import com.safeanot.app.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateBadgesUseCaseTest {

    private lateinit var fakeBadgeRepo: FakeBadgeRepository
    private lateinit var fakeStreakRepo: FakeStreakRepository
    private lateinit var fakeAuditRepo: FakeAuditRepository
    private lateinit var useCase: EvaluateBadgesUseCase

    @Before
    fun setup() {
        fakeBadgeRepo = FakeBadgeRepository()
        fakeStreakRepo = FakeStreakRepository()
        fakeAuditRepo = FakeAuditRepository()
        useCase = EvaluateBadgesUseCase(fakeBadgeRepo, fakeStreakRepo, fakeAuditRepo)
    }

    @Test
    fun `unlocks FIRST_SCAN when audit count greater than 0`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 1

        val result = useCase()

        assertTrue(result.contains(BadgeType.FIRST_SCAN))
        assertTrue(fakeBadgeRepo.unlockedBadges.contains(BadgeType.FIRST_SCAN))
    }

    @Test
    fun `does not unlock FIRST_SCAN when audit count is 0`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 0

        val result = useCase()

        assertTrue(!result.contains(BadgeType.FIRST_SCAN))
    }

    @Test
    fun `unlocks PHONE_HARDENED when score is 100`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 1
        fakeAuditRepo.scoreFlow.value = SecurityScore(scorePercent = 100, totalItems = 10, securedItems = 10)

        val result = useCase()

        assertTrue(result.contains(BadgeType.PHONE_HARDENED))
    }

    @Test
    fun `does not unlock PHONE_HARDENED when score below 100`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 1
        fakeAuditRepo.scoreFlow.value = SecurityScore(scorePercent = 99, totalItems = 10, securedItems = 9)

        val result = useCase()

        assertTrue(!result.contains(BadgeType.PHONE_HARDENED))
    }

    @Test
    fun `unlocks STREAK_STARTER at streak 3`() = runTest {
        fakeStreakRepo.currentStreak = Streak(currentStreak = 3)

        val result = useCase()

        assertTrue(result.contains(BadgeType.STREAK_STARTER))
    }

    @Test
    fun `does not unlock STREAK_STARTER at streak 2`() = runTest {
        fakeStreakRepo.currentStreak = Streak(currentStreak = 2)

        val result = useCase()

        assertTrue(!result.contains(BadgeType.STREAK_STARTER))
    }

    @Test
    fun `unlocks WEEK_WARRIOR at streak 7`() = runTest {
        fakeStreakRepo.currentStreak = Streak(currentStreak = 7)

        val result = useCase()

        assertTrue(result.contains(BadgeType.WEEK_WARRIOR))
    }

    @Test
    fun `unlocks MONTH_MASTER at streak 30`() = runTest {
        fakeStreakRepo.currentStreak = Streak(currentStreak = 30)

        val result = useCase()

        assertTrue(result.contains(BadgeType.MONTH_MASTER))
    }

    @Test
    fun `unlocks multiple streak badges at once when streak is 30`() = runTest {
        fakeStreakRepo.currentStreak = Streak(currentStreak = 30)

        val result = useCase()

        assertTrue(result.contains(BadgeType.STREAK_STARTER))
        assertTrue(result.contains(BadgeType.WEEK_WARRIOR))
        assertTrue(result.contains(BadgeType.MONTH_MASTER))
    }

    @Test
    fun `idempotent re-evaluation returns empty when already unlocked`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 1
        fakeStreakRepo.currentStreak = Streak(currentStreak = 3)

        val firstResult = useCase()
        assertTrue(firstResult.isNotEmpty())

        // Second call - already unlocked badges return false from unlockBadge
        val secondResult = useCase()
        assertTrue(secondResult.isEmpty())
    }

    @Test
    fun `returns only newly unlocked badges`() = runTest {
        fakeAuditRepo.auditCountFlow.value = 1
        fakeStreakRepo.currentStreak = Streak(currentStreak = 3)

        val result = useCase()

        // Should contain FIRST_SCAN and STREAK_STARTER
        assertEquals(2, result.size)
        assertTrue(result.contains(BadgeType.FIRST_SCAN))
        assertTrue(result.contains(BadgeType.STREAK_STARTER))
    }

    // --- Fakes ---

    private class FakeBadgeRepository : BadgeRepository {
        val unlockedBadges = mutableSetOf<BadgeType>()

        override fun observeAllBadges(): Flow<List<BadgeProgress>> = flowOf(emptyList())
        override fun observeUnlockedCount(): Flow<Int> = flowOf(unlockedBadges.size)

        override suspend fun unlockBadge(type: BadgeType): Boolean {
            return if (type !in unlockedBadges) {
                unlockedBadges.add(type)
                true
            } else {
                false
            }
        }
    }

    private class FakeStreakRepository : StreakRepository {
        var currentStreak = Streak()

        override fun observeStreak(): Flow<Streak> = flowOf(currentStreak)
        override suspend fun getStreak(): Streak = currentStreak
        override suspend fun updateStreak(streak: Streak) {
            currentStreak = streak
        }
    }

    private class FakeAuditRepository : AuditRepository {
        val auditCountFlow = MutableStateFlow(0)
        val scoreFlow = MutableStateFlow<SecurityScore?>(null)

        override fun getAllAuditItems(): Flow<List<AuditItem>> = flowOf(emptyList())
        override fun getAuditItemsByCategory(category: String): Flow<List<AuditItem>> = flowOf(emptyList())
        override fun getSecurityScore(): Flow<SecurityScore?> = scoreFlow
        override suspend fun runAudit() {}
        override suspend fun runAuditAndDetectChanges(): AuditChangeSummary = AuditChangeSummary(emptyList(), emptyList())
        override suspend fun updateItemStatus(id: Int, status: AuditStatus) {}
        override suspend fun updateItemStatusByPackage(packageName: String, status: AuditStatus) {}
        override suspend fun recalculateScore() {}
        override fun getCompletedAuditCount(): Flow<Int> = auditCountFlow
        override fun getLastAuditTimestamp(): Flow<Long?> = flowOf(null)
    }
}
