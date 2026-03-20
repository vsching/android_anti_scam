/**
 * Concrete implementation of StreakRepository backed by Room.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.StreakDao
import com.safeanot.app.data.local.entity.StreakEntity
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StreakRepositoryImpl @Inject constructor(
    private val streakDao: StreakDao,
) : StreakRepository {

    override fun observeStreak(): Flow<Streak> {
        return streakDao.observeStreak().map { entity ->
            entity?.toDomain() ?: Streak()
        }
    }

    override suspend fun getStreak(): Streak {
        return streakDao.getStreakOnce()?.toDomain() ?: Streak()
    }

    override suspend fun updateStreak(streak: Streak) {
        streakDao.upsert(streak.toEntity())
    }

    private fun StreakEntity.toDomain(): Streak = Streak(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCheckDate = lastCheckDate,
        streakStartDate = streakStartDate,
    )

    private fun Streak.toEntity(): StreakEntity = StreakEntity(
        id = 1,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastCheckDate = lastCheckDate,
        streakStartDate = streakStartDate,
    )
}
