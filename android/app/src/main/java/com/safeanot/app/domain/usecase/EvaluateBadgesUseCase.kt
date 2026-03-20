/**
 * Use case that evaluates badge conditions and unlocks eligible badges.
 * Checks score-based badges (FIRST_SCAN, PHONE_HARDENED) and streak-based badges
 * (STREAK_STARTER, WEEK_WARRIOR, MONTH_MASTER).
 * Feature-triggered badges (LINK_CHECKER, SHARE_GUARDIAN, FAMILY_PROTECTOR, SCAM_SPOTTER)
 * are unlocked by their respective features calling UnlockBadgeUseCase directly.
 * Returns list of newly unlocked badges (may be empty).
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.BadgeRepository
import com.safeanot.app.domain.repository.StreakRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

open class EvaluateBadgesUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
    private val streakRepository: StreakRepository,
    private val auditRepository: AuditRepository,
) {

    open suspend operator fun invoke(): List<BadgeType> {
        val newlyUnlocked = mutableListOf<BadgeType>()

        // FIRST_SCAN: has any completed audit (audit count > 0)
        val auditCount = auditRepository.getCompletedAuditCount().first()
        if (auditCount > 0) {
            if (badgeRepository.unlockBadge(BadgeType.FIRST_SCAN)) {
                newlyUnlocked.add(BadgeType.FIRST_SCAN)
            }
        }

        // PHONE_HARDENED: current security score == 100
        val securityScore = auditRepository.getSecurityScore().first()
        if (securityScore != null && securityScore.scorePercent == 100) {
            if (badgeRepository.unlockBadge(BadgeType.PHONE_HARDENED)) {
                newlyUnlocked.add(BadgeType.PHONE_HARDENED)
            }
        }

        // Streak-based badges
        val streak = streakRepository.getStreak()

        if (streak.currentStreak >= 3) {
            if (badgeRepository.unlockBadge(BadgeType.STREAK_STARTER)) {
                newlyUnlocked.add(BadgeType.STREAK_STARTER)
            }
        }

        if (streak.currentStreak >= 7) {
            if (badgeRepository.unlockBadge(BadgeType.WEEK_WARRIOR)) {
                newlyUnlocked.add(BadgeType.WEEK_WARRIOR)
            }
        }

        if (streak.currentStreak >= 30) {
            if (badgeRepository.unlockBadge(BadgeType.MONTH_MASTER)) {
                newlyUnlocked.add(BadgeType.MONTH_MASTER)
            }
        }

        return newlyUnlocked
    }
}
