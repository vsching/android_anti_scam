package com.safeanot.app.data.repository

import com.safeanot.app.data.local.StreakDao
import com.safeanot.app.data.local.entity.StreakEntity
import com.safeanot.app.domain.model.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StreakRepositoryImplTest {

    private lateinit var fakeDao: FakeStreakDao
    private lateinit var repository: StreakRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeStreakDao()
        repository = StreakRepositoryImpl(fakeDao)
    }

    @Test
    fun `observeStreak returns default when no entity exists`() = runTest {
        val result = repository.observeStreak().first()
        assertEquals(Streak(), result)
    }

    @Test
    fun `observeStreak maps entity to domain model`() = runTest {
        fakeDao.entityFlow.value = StreakEntity(
            id = 1,
            currentStreak = 5,
            longestStreak = 10,
            lastCheckDate = 12345L,
            streakStartDate = 11111L,
        )

        val result = repository.observeStreak().first()
        assertEquals(5, result.currentStreak)
        assertEquals(10, result.longestStreak)
        assertEquals(12345L, result.lastCheckDate)
        assertEquals(11111L, result.streakStartDate)
    }

    @Test
    fun `getStreak returns default when no entity exists`() = runTest {
        val result = repository.getStreak()
        assertEquals(Streak(), result)
    }

    @Test
    fun `getStreak maps entity to domain model`() = runTest {
        fakeDao.storedEntity = StreakEntity(
            id = 1,
            currentStreak = 3,
            longestStreak = 7,
            lastCheckDate = 99999L,
            streakStartDate = 88888L,
        )

        val result = repository.getStreak()
        assertEquals(3, result.currentStreak)
        assertEquals(7, result.longestStreak)
        assertEquals(99999L, result.lastCheckDate)
        assertEquals(88888L, result.streakStartDate)
    }

    @Test
    fun `updateStreak maps domain model to entity and upserts`() = runTest {
        val streak = Streak(
            currentStreak = 4,
            longestStreak = 8,
            lastCheckDate = 55555L,
            streakStartDate = 44444L,
        )

        repository.updateStreak(streak)

        val stored = fakeDao.storedEntity!!
        assertEquals(1, stored.id)
        assertEquals(4, stored.currentStreak)
        assertEquals(8, stored.longestStreak)
        assertEquals(55555L, stored.lastCheckDate)
        assertEquals(44444L, stored.streakStartDate)
    }

    // --- Fake ---

    private class FakeStreakDao : StreakDao {
        val entityFlow = MutableStateFlow<StreakEntity?>(null)
        var storedEntity: StreakEntity? = null

        override fun observeStreak(): Flow<StreakEntity?> = entityFlow

        override suspend fun getStreakOnce(): StreakEntity? = storedEntity

        override suspend fun upsert(entity: StreakEntity) {
            storedEntity = entity
            entityFlow.value = entity
        }
    }
}
