/**
 * Concrete implementation of QuizRepository backed by Room.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.QuizDao
import com.safeanot.app.data.local.entity.QuizResultEntity
import com.safeanot.app.domain.model.QuizResult
import com.safeanot.app.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val quizDao: QuizDao,
) : QuizRepository {

    override suspend fun saveResult(
        sessionId: String,
        scorePercent: Int,
        correctCount: Int,
        questionCount: Int,
    ) {
        quizDao.insert(
            QuizResultEntity(
                sessionId = sessionId,
                scorePercent = scorePercent,
                correctCount = correctCount,
                questionCount = questionCount,
                completedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun observeResults(): Flow<List<QuizResult>> {
        return quizDao.observeResults().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBestScore(): Int? {
        return quizDao.getBestScore()
    }

    private fun QuizResultEntity.toDomain(): QuizResult = QuizResult(
        id = id,
        sessionId = sessionId,
        scorePercent = scorePercent,
        correctCount = correctCount,
        questionCount = questionCount,
        completedAt = completedAt,
    )
}
