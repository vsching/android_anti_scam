package com.safeanot.app.data.repository

import com.safeanot.app.data.local.QuizDao
import com.safeanot.app.data.local.entity.QuizResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class QuizRepositoryImplTest {

    private lateinit var fakeDao: FakeQuizDao
    private lateinit var repository: QuizRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeQuizDao()
        repository = QuizRepositoryImpl(fakeDao)
    }

    @Test
    fun `saveResult inserts entity with correct fields`() = runTest {
        repository.saveResult(
            sessionId = "session-1",
            scorePercent = 80,
            correctCount = 4,
            questionCount = 5,
        )

        assertEquals(1, fakeDao.insertedEntities.size)
        val entity = fakeDao.insertedEntities.first()
        assertEquals("session-1", entity.sessionId)
        assertEquals(80, entity.scorePercent)
        assertEquals(4, entity.correctCount)
        assertEquals(5, entity.questionCount)
    }

    @Test
    fun `observeResults maps entities to domain models`() = runTest {
        fakeDao.resultsFlow.value = listOf(
            QuizResultEntity(
                id = 1,
                sessionId = "s1",
                scorePercent = 100,
                correctCount = 5,
                questionCount = 5,
                completedAt = 12345L,
            ),
        )

        val results = repository.observeResults().first()

        assertEquals(1, results.size)
        val result = results.first()
        assertEquals("s1", result.sessionId)
        assertEquals(100, result.scorePercent)
        assertEquals(12345L, result.completedAt)
    }

    @Test
    fun `getBestScore returns null when no results`() = runTest {
        val best = repository.getBestScore()
        assertNull(best)
    }

    @Test
    fun `getBestScore returns highest score`() = runTest {
        fakeDao.bestScore = 80
        val best = repository.getBestScore()
        assertEquals(80, best)
    }

    // --- Fake ---

    private class FakeQuizDao : QuizDao {
        val insertedEntities = mutableListOf<QuizResultEntity>()
        val resultsFlow = MutableStateFlow<List<QuizResultEntity>>(emptyList())
        var bestScore: Int? = null

        override suspend fun insert(result: QuizResultEntity) {
            insertedEntities.add(result)
        }

        override fun observeResults(): Flow<List<QuizResultEntity>> = resultsFlow

        override suspend fun getBestScore(): Int? = bestScore
    }
}
