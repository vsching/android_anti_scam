/**
 * Repository interface for streak data operations.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.Streak
import kotlinx.coroutines.flow.Flow

interface StreakRepository {

    /** Observe the current streak as a reactive Flow. */
    fun observeStreak(): Flow<Streak>

    /** Get the current streak snapshot. */
    suspend fun getStreak(): Streak

    /** Update the streak data. */
    suspend fun updateStreak(streak: Streak)
}
