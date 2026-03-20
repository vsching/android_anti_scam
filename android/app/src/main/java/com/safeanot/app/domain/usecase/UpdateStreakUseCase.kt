/**
 * Use case that updates the user's streak based on the current security score.
 * Converts millis to day units for comparison to avoid date-unit mismatch.
 * After updating the streak, evaluates badges so streak-based badges see the fresh count.
 * Returns list of newly unlocked badges (may be empty).
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.repository.StreakRepository
import javax.inject.Inject

class UpdateStreakUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val evaluateBadgesUseCase: EvaluateBadgesUseCase,
) {

    companion object {
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        private const val SCORE_THRESHOLD = 80
    }

    suspend operator fun invoke(currentScorePercent: Int): List<BadgeType> {
        val streak = streakRepository.getStreak()
        val now = System.currentTimeMillis()
        val lastDay = streak.lastCheckDate / MILLIS_PER_DAY
        val today = now / MILLIS_PER_DAY

        if (lastDay == today) {
            // Already checked today -- skip streak update but still evaluate badges
            return evaluateBadgesUseCase()
        }

        val updatedStreak = if (currentScorePercent >= SCORE_THRESHOLD) {
            if (lastDay == today - 1 || streak.currentStreak == 0) {
                // Consecutive day or fresh start
                val newCurrent = streak.currentStreak + 1
                val newLongest = maxOf(streak.longestStreak, newCurrent)
                val newStartDate = if (streak.currentStreak == 0) now else streak.streakStartDate
                Streak(
                    currentStreak = newCurrent,
                    longestStreak = newLongest,
                    lastCheckDate = now,
                    streakStartDate = newStartDate,
                )
            } else {
                // Missed day(s) -- reset streak to 1 (starting fresh today)
                Streak(
                    currentStreak = 1,
                    longestStreak = maxOf(streak.longestStreak, 1),
                    lastCheckDate = now,
                    streakStartDate = now,
                )
            }
        } else {
            // Score below threshold -- reset streak
            Streak(
                currentStreak = 0,
                longestStreak = streak.longestStreak,
                lastCheckDate = now,
                streakStartDate = 0L,
            )
        }

        streakRepository.updateStreak(updatedStreak)

        // Evaluate badges AFTER streak update so streak-based badges see the fresh count
        return evaluateBadgesUseCase()
    }
}
