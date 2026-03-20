package com.safeanot.app.feature.achievements

import com.safeanot.app.domain.model.BadgeInfo
import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.usecase.GetBadgesUseCase
import com.safeanot.app.domain.usecase.GetCurrentStreakUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val badgesFlow = MutableStateFlow<List<BadgeProgress>>(emptyList())
    private val streakFlow = MutableStateFlow(Streak())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AchievementsViewModel {
        val getBadgesUseCase = object : GetBadgesUseCase(
            object : com.safeanot.app.domain.repository.BadgeRepository {
                override fun observeAllBadges() = badgesFlow
                override fun observeUnlockedCount() = MutableStateFlow(0)
                override suspend fun unlockBadge(type: BadgeType) = true
            }
        ) {
            override fun invoke(): Flow<List<BadgeProgress>> = badgesFlow
        }

        val getCurrentStreakUseCase = object : GetCurrentStreakUseCase(
            object : com.safeanot.app.domain.repository.StreakRepository {
                override fun observeStreak() = streakFlow
                override suspend fun getStreak() = streakFlow.value
                override suspend fun updateStreak(streak: Streak) {}
            }
        ) {
            override fun invoke(): Flow<Streak> = streakFlow
        }

        return AchievementsViewModel(getBadgesUseCase, getCurrentStreakUseCase)
    }

    @Test
    fun `initial state shows empty badges and default streak`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(emptyList<BadgeProgress>(), state.badges)
        assertEquals(0, state.streak.currentStreak)
    }

    @Test
    fun `badges are updated when flow emits`() = runTest {
        val viewModel = createViewModel()

        val testBadges = listOf(
            BadgeProgress(
                badge = BadgeInfo(
                    type = BadgeType.FIRST_SCAN,
                    title = "First Scan",
                    description = "Completed first scan",
                    conditionHint = "Complete a scan",
                    icon = "search",
                ),
                isUnlocked = true,
                unlockedAt = 12345L,
            ),
        )
        badgesFlow.value = testBadges
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.badges.size)
        assertEquals(true, viewModel.uiState.value.badges.first().isUnlocked)
    }

    @Test
    fun `streak is updated when flow emits`() = runTest {
        val viewModel = createViewModel()

        streakFlow.value = Streak(currentStreak = 5, longestStreak = 10)
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.streak.currentStreak)
        assertEquals(10, viewModel.uiState.value.streak.longestStreak)
    }
}
