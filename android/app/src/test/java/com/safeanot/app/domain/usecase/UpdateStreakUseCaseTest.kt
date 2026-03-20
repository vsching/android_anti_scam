package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UpdateStreakUseCaseTest {

    private lateinit var fakeStreakRepo: FakeStreakRepository
    private lateinit var fakeEvaluateBadges: FakeEvaluateBadgesUseCase
    private lateinit var useCase: UpdateStreakUseCase

    companion object {
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    @Before
    fun setup() {
        fakeStreakRepo = FakeStreakRepository()
        fakeEvaluateBadges = FakeEvaluateBadgesUseCase()
        useCase = UpdateStreakUseCase(fakeStreakRepo, fakeEvaluateBadges)
    }

    @Test
    fun `increments streak on consecutive day with score above 80`() = runTest {
        val yesterday = System.currentTimeMillis() - MILLIS_PER_DAY
        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 3,
            longestStreak = 5,
            lastCheckDate = yesterday,
            streakStartDate = yesterday - 2 * MILLIS_PER_DAY,
        )

        useCase(85)

        val updated = fakeStreakRepo.currentStreak
        assertEquals(4, updated.currentStreak)
        assertEquals(5, updated.longestStreak)
    }

    @Test
    fun `updates longest streak when current exceeds it`() = runTest {
        val yesterday = System.currentTimeMillis() - MILLIS_PER_DAY
        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 5,
            longestStreak = 5,
            lastCheckDate = yesterday,
            streakStartDate = yesterday - 4 * MILLIS_PER_DAY,
        )

        useCase(90)

        val updated = fakeStreakRepo.currentStreak
        assertEquals(6, updated.currentStreak)
        assertEquals(6, updated.longestStreak)
    }

    @Test
    fun `resets streak to 0 when score below 80`() = runTest {
        val yesterday = System.currentTimeMillis() - MILLIS_PER_DAY
        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 5,
            longestStreak = 10,
            lastCheckDate = yesterday,
            streakStartDate = yesterday - 4 * MILLIS_PER_DAY,
        )

        useCase(50)

        val updated = fakeStreakRepo.currentStreak
        assertEquals(0, updated.currentStreak)
        assertEquals(10, updated.longestStreak)
    }

    @Test
    fun `resets streak to 1 when missed day with score above 80`() = runTest {
        val twoDaysAgo = System.currentTimeMillis() - 2 * MILLIS_PER_DAY
        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 5,
            longestStreak = 10,
            lastCheckDate = twoDaysAgo,
            streakStartDate = twoDaysAgo - 4 * MILLIS_PER_DAY,
        )

        useCase(85)

        val updated = fakeStreakRepo.currentStreak
        assertEquals(1, updated.currentStreak)
        assertEquals(10, updated.longestStreak)
    }

    @Test
    fun `skips streak update on same day but still evaluates badges`() = runTest {
        val now = System.currentTimeMillis()
        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 3,
            longestStreak = 5,
            lastCheckDate = now,
            streakStartDate = now - 2 * MILLIS_PER_DAY,
        )
        fakeEvaluateBadges.result = listOf(BadgeType.FIRST_SCAN)

        val result = useCase(90)

        // Streak unchanged
        assertEquals(3, fakeStreakRepo.currentStreak.currentStreak)
        // But badges evaluated
        assertEquals(listOf(BadgeType.FIRST_SCAN), result)
        assertEquals(1, fakeEvaluateBadges.callCount)
    }

    @Test
    fun `starts fresh streak from zero with score above 80`() = runTest {
        // No previous streak (all defaults)
        fakeStreakRepo.currentStreak = Streak()

        useCase(85)

        val updated = fakeStreakRepo.currentStreak
        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.longestStreak)
        assert(updated.streakStartDate > 0)
    }

    @Test
    fun `day comparison uses millis divided by MILLIS_PER_DAY`() = runTest {
        // Set lastCheckDate just before midnight boundary (same day as now in day units)
        val now = System.currentTimeMillis()
        val today = now / MILLIS_PER_DAY
        val sameDayEarlier = today * MILLIS_PER_DAY + 1000L // early in the same day

        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 3,
            longestStreak = 5,
            lastCheckDate = sameDayEarlier,
            streakStartDate = sameDayEarlier - 2 * MILLIS_PER_DAY,
        )

        useCase(90)

        // Should skip because same day
        assertEquals(3, fakeStreakRepo.currentStreak.currentStreak)
    }

    @Test
    fun `returns newly unlocked badges from evaluateBadgesUseCase`() = runTest {
        val yesterday = System.currentTimeMillis() - MILLIS_PER_DAY
        fakeStreakRepo.currentStreak = Streak(
            currentStreak = 2,
            longestStreak = 2,
            lastCheckDate = yesterday,
            streakStartDate = yesterday - MILLIS_PER_DAY,
        )
        fakeEvaluateBadges.result = listOf(BadgeType.STREAK_STARTER)

        val result = useCase(85)

        assertEquals(listOf(BadgeType.STREAK_STARTER), result)
    }

    // --- Fakes ---

    private class FakeStreakRepository : StreakRepository {
        var currentStreak = Streak()
        private val flow = MutableStateFlow(Streak())

        override fun observeStreak(): Flow<Streak> = flow
        override suspend fun getStreak(): Streak = currentStreak
        override suspend fun updateStreak(streak: Streak) {
            currentStreak = streak
            flow.value = streak
        }
    }

    private class FakeEvaluateBadgesUseCase : EvaluateBadgesUseCase() {
        var result: List<BadgeType> = emptyList()
        var callCount = 0

        override suspend fun invoke(): List<BadgeType> {
            callCount++
            return result
        }
    }
}
