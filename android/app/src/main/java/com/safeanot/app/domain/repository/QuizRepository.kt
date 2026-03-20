/**
 * Repository interface for quiz result data operations.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.QuizResult
import kotlinx.coroutines.flow.Flow

interface QuizRepository {

    /** Save a completed quiz result. */
    suspend fun saveResult(
        sessionId: String,
        scorePercent: Int,
        correctCount: Int,
        questionCount: Int,
    )

    /** Observe all quiz results ordered by most recent first. */
    fun observeResults(): Flow<List<QuizResult>>

    /** Get the highest score achieved across all quiz sessions. */
    suspend fun getBestScore(): Int?
}
