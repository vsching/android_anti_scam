/**
 * Repository interface for badge data operations.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.model.BadgeType
import kotlinx.coroutines.flow.Flow

interface BadgeRepository {

    /** Observe all badges with their unlock state. */
    fun observeAllBadges(): Flow<List<BadgeProgress>>

    /** Observe the count of unlocked badges. */
    fun observeUnlockedCount(): Flow<Int>

    /**
     * Unlock a badge by type. Returns true if newly unlocked, false if already unlocked.
     */
    suspend fun unlockBadge(type: BadgeType): Boolean
}
