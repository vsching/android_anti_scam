/**
 * Use case that updates the user's streak based on the current security score.
 * Uses LocalDate for timezone-aware day comparison to avoid UTC midnight edge cases.
 * After updating the streak, evaluates badges so streak-based badges see the fresh count.
 * Returns list of newly unlocked badges (may be empty).
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.repository.StreakRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class UpdateStreakUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val evaluateBadgesUseCase: EvaluateBadgesUseCase,
) {

    companion object {
        private const val SCORE_THRESHOLD = 80
    }

    suspend operator fun invoke(currentScorePercent: Int): List<BadgeType> {
        val streak = streakRepository.getStreak()
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val lastDay = Instant.ofEpochMilli(streak.lastCheckDate).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val daysBetween = ChronoUnit.DAYS.between(lastDay, today)

        if (daysBetween == 0L && streak.lastCheckDate != 0L) {
            // Already checked today -- skip streak update but still evaluate badges
            return evaluateBadgesUseCase()
        }

        val updatedStreak = if (currentScorePercent >= SCORE_THRESHOLD) {
            if (daysBetween == 1L || streak.currentStreak == 0) {
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
