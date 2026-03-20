/**
 * Use case that persists a completed quiz result.
 * Wraps repository call in try/catch so persistence failures don't crash the quiz flow.
 */
package com.safeanot.app.domain.usecase

import com.safeanot.app.domain.repository.QuizRepository
import javax.inject.Inject

class SaveQuizResultUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
) {

    /**
     * Saves the quiz result. Returns true on success, false if persistence failed.
     */
    suspend operator fun invoke(
        sessionId: String,
        scorePercent: Int,
        correctCount: Int,
        questionCount: Int,
    ): Boolean {
        return try {
            quizRepository.saveResult(
                sessionId = sessionId,
                scorePercent = scorePercent,
                correctCount = correctCount,
                questionCount = questionCount,
            )
            true
        } catch (_: Exception) {
            // Persistence failure is non-critical; quiz UI still shows results
            false
        }
    }
}
