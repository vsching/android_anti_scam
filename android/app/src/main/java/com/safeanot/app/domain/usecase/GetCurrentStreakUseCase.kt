/**
 * Use case that observes the current streak as a reactive Flow.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentStreakUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
) {
    operator fun invoke(): Flow<Streak> = streakRepository.observeStreak()
}
